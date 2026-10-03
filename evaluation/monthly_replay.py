"""Separate monthly experiment. Same frozen policy, only matured labels at each origin."""
import argparse
import csv
import datetime as dt
import hashlib
import json
import pickle
from pathlib import Path
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from fixed_replay import HOUR, TEST_START, CUT, THRESHOLD, NAMES, read_data, variables, resolution, summary


def months():
    origin = TEST_START
    while origin < CUT:
        date = dt.datetime.fromtimestamp(origin/1000, dt.timezone.utc)
        following = dt.datetime(date.year+int(date.month==12), date.month%12+1, 1, tzinfo=dt.timezone.utc)
        end = min(int(following.timestamp()*1000), CUT)
        yield origin, end
        origin = end


def calibration_start(origin):
    date = dt.datetime.fromtimestamp(origin/1000, dt.timezone.utc)
    month_index = date.year*12+date.month-1-6
    return int(dt.datetime(month_index//12, month_index%12+1, 1, tzinfo=dt.timezone.utc).timestamp()*1000)


def training_phase(t, origin):
    # Never include labels which have not matured at the simulated origin.
    cutoff = origin - 24*HOUR
    start = calibration_start(origin)
    if t+24*HOUR < start:
        return 'TRAIN'
    if start <= t and t+24*HOUR < cutoff:
        return 'CALIBRATION'
    return None


def fit(arrays, baseline, origin, cache):
    dataset = dict(TRAIN=[], CALIBRATION=[])
    for t, row in sorted(baseline.items()):
        phase = training_phase(t, origin)
        if phase is None:
            continue
        assert t+24*HOUR < origin-24*HOUR
        if t not in cache:
            x = variables(arrays, t)
            labels = [resolution(arrays[NAMES[-1]], t, float(row['entry']), float(row['targetPct']), side)
                      for side in ('BUY','SELL')] if x is not None else None
            cache[t] = (x, labels)
        x, labels = cache[t]
        if x is not None:
            dataset[phase].append((t, x, labels))
    models, counts = [], {}
    for index, side in enumerate(('BUY','SELL')):
        tr = [r for r in dataset['TRAIN'] if r[2][index]['status'] in ('HIT','FAIL')]
        cal = [r for r in dataset['CALIBRATION'] if r[2][index]['status'] in ('HIT','FAIL')]
        y = [r[2][index]['status']=='HIT' for r in tr]
        yc = [r[2][index]['status']=='HIT' for r in cal]
        if len(tr)<100 or len(cal)<100 or len(set(y))<2 or len(set(yc))<2:
            raise ValueError('Insufficient known training/calibration classes')
        model = GradientBoostingClassifier(n_estimators=40, max_depth=2, learning_rate=.05, random_state=17)
        model.fit([r[1] for r in tr], y)
        calibrator = LogisticRegression(C=1, random_state=17)
        calibrator.fit(model.decision_function([r[1] for r in cal]).reshape(-1,1), yc)
        models.append((model, calibrator))
        counts[side] = dict(train=len(tr), calibration=len(cal), latest_train_label=max(r[0]+24*HOUR for r in tr),
                            latest_calibration_label=max(r[0]+24*HOUR for r in cal))
    return models, counts


def run(source, output, version):
    output.mkdir(parents=True, exist_ok=True)
    arrays, manifests = read_data(source/'replay-data')
    original = json.loads((source/'replay-output/model_manifest.json').read_text())
    blob = (source/'replay-output/frozen_model.pkl').read_bytes()
    if hashlib.sha256(blob).hexdigest()!=original['model_sha256']:
        raise ValueError('Original model hash mismatch')
    models = pickle.loads(blob)  # trusted, hash-verified artifact from our fixed experiment
    with (source/'replay-kotlin/kotlin_decisions.csv').open() as f:
        baseline = {int(r['signalCloseTime']):r for r in csv.DictReader(f)}
    with (source/'replay-kotlin/kotlin_accepted.csv').open() as f:
        accepted = {int(r['signalCloseTime']) for r in csv.DictReader(f)}
    fixed_decisions = {r['time']:r for r in map(json.loads,(source/'replay-output/decisions.jsonl').read_text().splitlines())}
    rows, model_registry, cache = [], [], {}
    counts = original['counts']
    with (output/'decisions.jsonl').open('w') as journal:
        for origin, end in months():
            if origin!=TEST_START:
                models, counts = fit(arrays, baseline, origin, cache)
                blob = pickle.dumps(models)
            model_hash = hashlib.sha256(blob).hexdigest()
            name = dt.datetime.fromtimestamp(origin/1000,dt.timezone.utc).strftime('%Y-%m')
            (output/('model_'+name+'.pkl')).write_bytes(blob)
            record = dict(origin=origin, until=end, sha256=model_hash, version=version, threshold=THRESHOLD,
                calibration_start=calibration_start(origin), latest_allowed_label=origin-24*HOUR,
                counts=counts, initial_model_reused=origin==TEST_START)
            for side in counts:
                assert counts[side]['latest_calibration_label'] < origin-24*HOUR
            model_registry.append(record)
            (output/('model_'+name+'_manifest.json')).write_text(json.dumps(record,indent=2)+'\n')
            print('Frozen monthly model '+name+' '+model_hash,flush=True)
            # Fit and persist model before predicting that month's examination decisions.
            for t in sorted(v for v in fixed_decisions if origin<=v<end):
                old = fixed_decisions[t]
                x = old['variables']
                probabilities = [float(cal.predict_proba(model.decision_function([x]).reshape(-1,1))[0,1])
                                 for model,cal in models] if x is not None else None
                side = ('BUY','SELL')[int(np.argmax(probabilities))] if probabilities else 'WAIT'
                monthly = dict(direction=side, probability=max(probabilities) if probabilities else None,
                    direction_probabilities=probabilities, accepted=max(probabilities)>=THRESHOLD if probabilities else False,
                    rejection='BELOW_FROZEN_THRESHOLD' if probabilities else 'INVALID_OR_INSUFFICIENT_CONTEXT')
                row = dict(time=t, version=version, model_sha256=model_hash, model_origin=origin,
                    entry=old['entry'], target_pct=old['target_pct'], horizon_end=t+24*HOUR,
                    variables=x, news='absent_neutral', monthly=monthly,
                    kotlin=old['kotlin'], alternative=old['alternative'])
                journal.write(json.dumps(row)+'\n');journal.flush();rows.append(row)
    # Examination labels are revealed to reporting only after all predictions were persisted.
    fixed_outcomes = {r['time']:r for r in map(json.loads,(source/'replay-output/outcomes.jsonl').read_text().splitlines())}
    with (output/'outcomes.jsonl').open('w') as journal:
        for r in rows:
            result = resolution(arrays[NAMES[-1]],r['time'],r['entry'],r['target_pct'],r['monthly']['direction']) if r['entry'] is not None and r['monthly']['direction']!='WAIT' else dict(status='PENDING',reason='NO_DIRECTION_OR_CONTEXT')
            r['monthly']['result']=result
            for e in ('kotlin','alternative'):
                r[e]['result']=fixed_outcomes[r['time']][e]
            journal.write(json.dumps(dict(time=r['time'],monthly=result))+'\n')
    def metrics(group, engine):
        if engine!='monthly':return summary(group,engine)
        # Reuse exactly the fixed experiment's alternative metrics definitions.
        return summary([{**r,'alternative':r['monthly']} for r in group],'alternative')
    pairs=[r for r in rows if r['alternative']['result']['status'] in ('HIT','FAIL') and
            r['monthly']['result']['status'] in ('HIT','FAIL')]
    brier=lambda engine:float(np.mean([(r[engine]['probability']-(r[engine]['result']['status']=='HIT'))**2 for r in pairs])) if pairs else None
    report=dict(status='SEPARATE_EXPLORATORY_MONTHLY_RECONSTRUCTION', version=version,
        source_fixed_model=original['model_sha256'], source_run=37104820553, source_artifact=11267117887,
        threshold=THRESHOLD, models=model_registry,
        inputs={n:m['csv_sha256'] for n,m in manifests.items()},
        kotlin=metrics(rows,'kotlin'), alternative_fixed=metrics(rows,'alternative'), alternative_monthly=metrics(rows,'monthly'),
        paired_common_resolved=dict(cases=len(pairs), fixed_brier=brier('alternative'), monthly_brier=brier('monthly')),
        by_month={dt.datetime.fromtimestamp(o/1000,dt.timezone.utc).strftime('%Y-%m'):
            {e:metrics([r for r in rows if o<=r['time']<end],e) for e in ('kotlin','alternative','monthly')}
            for o,end in months()},
        limitations=['Same historical examination already inspected: not independent confirmation of production advantage.',
            'Only retraining cadence changes. Same features, learner, threshold65%, targets, horizon and news handling.',
            'First month reuses exact fixed model; subsequent months expanding training and latest six-calendar-month calibration.',
            '24h label embargo plus 24h before each model origin. Each model frozen for the entire month.',
            'Overlapping outcomes are correlated. Brier differences alone do not establish a precision improvement.',
            'Cost sensitivity is not an execution or profitability simulation. No Android activation.'])
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ('by_month','models','inputs')},indent=2))


if __name__=='__main__':
    p=argparse.ArgumentParser()
    for name in ('source','output','version'):p.add_argument('--'+name,required=True)
    a=p.parse_args();run(Path(a.source),Path(a.output),a.version)
