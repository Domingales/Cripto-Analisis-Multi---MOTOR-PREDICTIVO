"""Fixed-date, paired historical reconstruction. No production integration."""
import argparse
import csv
import hashlib
import datetime as dt
import json
import pickle
from pathlib import Path
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression

HOUR = 3600000
TRAIN_END = 1680307200000  # 2023-04-01 UTC; calibration begins here
TEST_START = 1696204800000  # 2023-10-02 UTC
CUT = 1790953200000  # 2026-10-02 15:00 UTC
CAL_END = TEST_START - 24 * HOUR  # additional embargo before examination
THRESHOLD = .65
NAMES = ('ADAUSDT_1h', 'ADAUSDT_4h', 'ADAUSDT_1d', 'BTCUSDT_4h', 'ADAUSDT_5m')


def read_data(root):
    arrays, manifests = {}, {}
    for name in NAMES:
        path = root / (name + '.csv')
        manifest = json.loads((root / (name + '_manifest.json')).read_text())
        if hashlib.sha256(path.read_bytes()).hexdigest() != manifest['csv_sha256']:
            raise ValueError('Input hash mismatch: ' + name)
        a = np.loadtxt(path, delimiter=',', skiprows=1, ndmin=2)
        if len(a) != manifest['count'] or a.shape[1] != 7 or not np.isfinite(a).all():
            raise ValueError('Invalid shape/values: ' + name)
        step = {'1h': HOUR, '4h': 4*HOUR, '1d': 24*HOUR, '5m': 300000}[name.split('_')[-1]]
        if (np.any(np.diff(a[:, 0]) <= 0) or np.any(a[:, 6] != a[:, 0]+step-1) or
                np.any(a[:, 0] % step) or np.any(a[:, 6] >= CUT)):
            raise ValueError('Invalid candle times: ' + name)
        arrays[name], manifests[name] = a, manifest
    return arrays, manifests


def closed_window(a, t, step):
    i = int(np.searchsorted(a[:, 6], t, side='right'))
    if i < 200:
        return None
    w = a[i-200:i]
    if w[-1, 6] != ((t+1)//step)*step-1 or np.any(np.diff(w[:, 0]) != step):
        return None
    return w


def variables(arrays, t):
    x = []
    for name, step in zip(NAMES[:4], (HOUR, 4*HOUR, 24*HOUR, 4*HOUR)):
        w = closed_window(arrays[name], t, step)
        if w is None:
            return None
        close, volumes = w[:, 4], w[:, 5]
        returns = np.diff(np.log(close))
        x.extend([float(close[-1]/close[-1-k]-1) for k in (1, 4, 12, 24)])
        x.extend([float(np.std(returns[-24:])), float(np.mean(w[-14:, 2]-w[-14:, 3])/close[-1]),
                  float(volumes[-1]/max(np.mean(volumes[-20:]), 1e-12)),
                  float(close[-1]/np.mean(close[-50:])-1), float(close[-1]/np.mean(close)-1)])
    return x  # market data only; no Kotlin score, confidence or decision variables


def resolution(fine, t, entry, target, side, cutoff=CUT):
    start, end = t+1, t+24*HOUR
    if end >= cutoff:
        return dict(status='PENDING', reason='WINDOW_NOT_EXPIRED')
    i = int(np.searchsorted(fine[:, 0], start))
    w = fine[i:i+288]
    if len(w) != 288 or not np.array_equal(w[:, 0], np.arange(start, end+1, 300000)):
        return dict(status='PENDING', reason='MISSING_5M_DATA')
    sell = side == 'SELL'
    favorable = ((entry-w[:, 3]) if sell else (w[:, 2]-entry))/entry*100
    adverse = ((entry-w[:, 2]) if sell else (w[:, 3]-entry))/entry*100
    touched = np.flatnonzero((favorable >= target) | (adverse <= -target))
    status, reason, when = 'NEUTRAL', 'WINDOW_EXPIRED', end
    if len(touched):
        j = int(touched[0])
        hit, fail = favorable[j] >= target, adverse[j] <= -target
        status = 'NEUTRAL' if hit and fail else 'HIT' if hit else 'FAIL'
        reason = 'BOTH_TOUCHED_SAME_5M_CANDLE' if hit and fail else 'TARGET_FIRST' if hit else 'STOP_FIRST'
        when = int(w[j, 6])
    return dict(status=status, reason=reason, outcome_time=when,
                mfe=float(max(0, max(favorable))), mae=float(min(0, min(adverse))))


def partition(t):
    # Labels must have matured strictly before the next boundary; no cross-boundary labels.
    if t+24*HOUR < TRAIN_END:
        return 'TRAIN'
    if TRAIN_END <= t and t+24*HOUR < CAL_END:
        return 'CALIBRATION'
    if t >= TEST_START:
        return 'EXAMINATION'
    return 'EMBARGO'


def summary(rows, engine):
    chosen = [r for r in rows if r[engine]['accepted']]
    resolved = [r for r in chosen if r[engine]['result']['status'] in ('HIT', 'FAIL')]
    statuses = {s: sum(r[engine]['result']['status'] == s for r in chosen)
                for s in ('HIT', 'FAIL', 'NEUTRAL', 'PENDING')}
    by_side = {}
    for side in ('BUY', 'SELL'):
        sub = [r for r in chosen if r[engine]['direction'] == side]
        by_side[side] = {s: sum(r[engine]['result']['status'] == s for r in sub)
                         for s in ('HIT', 'FAIL', 'NEUTRAL', 'PENDING')}
    out = dict(decisions=len(rows), signals=len(chosen), coverage=len(chosen)/len(rows) if rows else 0,
               resolved=len(resolved), **statuses,
               accuracy=statuses['HIT']/len(resolved) if resolved else None, directions=by_side)
    if engine == 'alternative':
        eligible = [r for r in rows if r[engine].get('probability') is not None and
                    r[engine]['result']['status'] in ('HIT', 'FAIL')]
        out['brier_all_resolved_chosen_directions'] = float(np.mean([
            (r[engine]['probability']-(r[engine]['result']['status']=='HIT'))**2 for r in eligible])) if eligible else None
        out['calibration_bins'] = [dict(low=low, high=low+.1,
            cases=len(group), mean_probability=float(np.mean([r[engine]['probability'] for r in group])) if group else None,
            observed_hit_rate=sum(r[engine]['result']['status']=='HIT' for r in group)/len(group) if group else None)
            for low in (0., .1, .2, .3, .4, .5, .6, .7, .8, .9)
            for group in [[r for r in eligible if low <= r[engine]['probability'] < low+.1]]]
    # Simple cost sensitivity on symmetric barrier returns; not an execution simulation.
    out['barrier_return_cost_sensitivity_pct'] = {str(bps): float(np.mean([
        (r['target_pct'] if r[engine]['result']['status']=='HIT' else -r['target_pct'])-bps/100
        for r in resolved])) if resolved else None for bps in (0, 5, 10, 20)}
    return out


def run(data, kotlin, output, version):
    output.mkdir(parents=True, exist_ok=True)
    arrays, manifests = read_data(data)
    with (kotlin / 'kotlin_decisions.csv').open() as f:
        baseline = {int(r['signalCloseTime']): r for r in csv.DictReader(f)}
    with (kotlin / 'kotlin_accepted.csv').open() as f:
        accepted = {int(r['signalCloseTime']) for r in csv.DictReader(f)}
    training = dict(TRAIN=[], CALIBRATION=[])
    # Only preparation periods are accessed to create labels before model freeze.
    for t, row in sorted(baseline.items()):
        phase = partition(t)
        if phase not in training:
            continue
        x = variables(arrays, t)
        if x is None:
            continue
        labels = [resolution(arrays[NAMES[-1]], t, float(row['entry']), float(row['targetPct']), side)
                  for side in ('BUY', 'SELL')]
        training[phase].append((t, x, labels))
    models, counts = [], {}
    for index, side in enumerate(('BUY', 'SELL')):
        tr = [r for r in training['TRAIN'] if r[2][index]['status'] in ('HIT','FAIL')]
        cal = [r for r in training['CALIBRATION'] if r[2][index]['status'] in ('HIT','FAIL')]
        y = [r[2][index]['status']=='HIT' for r in tr]
        yc = [r[2][index]['status']=='HIT' for r in cal]
        if len(tr)<100 or len(cal)<100 or len(set(y))<2 or len(set(yc))<2:
            raise ValueError('Insufficient training/calibration classes')
        model = GradientBoostingClassifier(n_estimators=40, max_depth=2, learning_rate=.05, random_state=17)
        model.fit([r[1] for r in tr], y)
        calibrator = LogisticRegression(C=1, random_state=17)
        calibrator.fit(model.decision_function([r[1] for r in cal]).reshape(-1, 1), yc)
        models.append((model, calibrator))
        counts[side] = dict(train=len(tr), calibration=len(cal), latest_train_label=max(r[0]+24*HOUR for r in tr),
                            latest_calibration_label=max(r[0]+24*HOUR for r in cal))
    blob = pickle.dumps(models)
    (output / 'frozen_model.pkl').write_bytes(blob)
    freeze = dict(version=version, model_sha256=hashlib.sha256(blob).hexdigest(), threshold=THRESHOLD,
                  train_end=TRAIN_END, calibration_end=CAL_END, examination_start=TEST_START, cutoff=CUT,
                  counts=counts, feature_names=[name+':'+feature for name in NAMES[:4] for feature in
                      ('return_1','return_4','return_12','return_24','volatility_24','range_14','volume_ratio_20','mean_distance_50','mean_distance_200')],
                  inputs={n: m['csv_sha256'] for n, m in manifests.items()},
                  status='HISTORICAL_RECONSTRUCTION_NOT_PROSPECTIVE')
    (output / 'model_manifest.json').write_text(json.dumps(freeze, indent=2)+'\n')
    # Freeze is written before scoring the examination; do not adjust using examination results.
    decisions, outcomes = [], []
    with (output / 'decisions.jsonl').open('w') as journal:
        for t in map(int, arrays['ADAUSDT_1h'][:, 6]):
            if not TEST_START <= t < CUT:
                continue
            row = baseline.get(t)
            x = variables(arrays, t) if row else None
            direction = 'WAIT'
            probs = None
            if x is not None:
                probs = [float(cal.predict_proba(model.decision_function([x]).reshape(-1,1))[0,1])
                         for model, cal in models]
                direction = ('BUY','SELL')[int(np.argmax(probs))]
            base_side = 'SELL' if row and 'SELL' in row['direction'] else 'BUY' if row and 'BUY' in row['direction'] else 'WAIT'
            record = dict(time=t, version=version, model_sha256=freeze['model_sha256'],
                entry=float(row['entry']) if row else None, target_pct=float(row['targetPct']) if row else None,
                horizon_end=t+24*HOUR, news='absent_neutral', variables=x,
                kotlin=dict(direction=base_side, accepted=t in accepted,
                    confidence=int(row['confidence']) if row else None,
                    probability=int(row['probability'])/100 if row and row['probability'] else None,
                    context=row, rejection='ENGINE_REJECTION_OR_COOLDOWN' if row else 'INVALID_OR_INSUFFICIENT_CONTEXT'),
                alternative=dict(direction=direction, probability=max(probs) if probs else None,
                    direction_probabilities=probs, accepted=max(probs)>=THRESHOLD if probs else False,
                    rejection='BELOW_FROZEN_THRESHOLD' if probs else 'INVALID_OR_INSUFFICIENT_CONTEXT'))
            journal.write(json.dumps(record)+'\n'); journal.flush()
            decisions.append(record)
    # All decisions are durable before labels for the examination are revealed to reporting.
    with (output / 'outcomes.jsonl').open('w') as journal:
        for r in decisions:
            for engine in ('kotlin','alternative'):
                result = resolution(arrays[NAMES[-1]], r['time'], r['entry'], r['target_pct'], r[engine]['direction']) if r['entry'] is not None and r[engine]['direction']!='WAIT' else dict(status='PENDING', reason='NO_DIRECTION_OR_CONTEXT')
                r[engine]['result'] = result
            journal.write(json.dumps(dict(time=r['time'], kotlin=r['kotlin']['result'], alternative=r['alternative']['result']))+'\n')
    paired = [r for r in decisions if r['kotlin']['accepted'] and r['alternative']['accepted'] and
              all(r[e]['result']['status'] in ('HIT','FAIL') for e in ('kotlin','alternative'))]
    report = dict(status='EXPLORATORY_FIXED_HISTORICAL_RECONSTRUCTION', freeze=freeze,
        kotlin=summary(decisions,'kotlin'), alternative=summary(decisions,'alternative'),
        paired_both_select=dict(cases=len(paired),
            kotlin_hits=sum(r['kotlin']['result']['status']=='HIT' for r in paired),
            alternative_hits=sum(r['alternative']['result']['status']=='HIT' for r in paired)),
        by_year={str(year): {e:summary([r for r in decisions if dt.datetime.fromtimestamp(r['time']/1000, dt.timezone.utc).year==year],e)
                            for e in ('kotlin','alternative')} for year in (2023,2024,2025,2026)},
        by_month={month: {e:summary([r for r in decisions if dt.datetime.fromtimestamp(r['time']/1000,dt.timezone.utc).strftime('%Y-%m')==month],e)
                          for e in ('kotlin','alternative')}
                  for month in sorted({dt.datetime.fromtimestamp(r['time']/1000,dt.timezone.utc).strftime('%Y-%m') for r in decisions})},
        limitations=['Code designed after 2023: historical reconstruction, not authentic prospective validation.',
          'No future news supplied; absent news excluded neutrally.',
          'Fixed Kotlin code learns only from already matured past patterns; alternative never retrained.',
          'Both engines use same entry, ATR target/stop and 24h horizon. Confidence is not probability.',
          'Overlapping horizons are correlated; no independent-case significance claim.',
          'Costs are illustrative barrier-return sensitivity, no spread/slippage/execution or profitability guarantee.',
          'Invalid context windows excluded without synthetic market data.',
          'No production improvement established: holdout not yet declared independent for future tuning.'])
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ('by_year','by_month','freeze')},indent=2))


if __name__ == '__main__':
    p=argparse.ArgumentParser()
    for name in ('data','kotlin','output','version'):
        p.add_argument('--'+name,required=True)
    a=p.parse_args()
    run(Path(a.data),Path(a.kotlin),Path(a.output),a.version)
