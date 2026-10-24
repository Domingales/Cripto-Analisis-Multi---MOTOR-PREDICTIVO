"""Independent market-data classifier. Research only, no Android integration."""
import argparse
import csv
import json
from pathlib import Path
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import brier_score_loss

HOUR = 3600000


def features(candles, i):
    window = candles[max(0, i-199):i+1]
    closes = np.array([r['close'] for r in window])
    volumes = np.array([r['volume'] for r in window])
    ranges = np.array([r['high']-r['low'] for r in window])
    returns = np.diff(np.log(closes))
    return [closes[-1]/closes[-1-k]-1 for k in (1, 4, 12, 24)] + [
        float(np.std(returns[-24:])), float(np.mean(ranges[-14:])/closes[-1]),
        float(volumes[-1]/max(np.mean(volumes[-20:]), 1e-12)),
        float(closes[-1]/np.mean(closes[-50:])-1),
        float(closes[-1]/np.mean(closes)-1)]


def outcome(fine, entry, target, direction):
    sign = 1 if direction == 'BUY' else -1
    take, stop = entry*(1+sign*target/100), entry*(1-sign*target/100)
    for r in fine:
        hit = r['high'] >= take if sign == 1 else r['low'] <= take
        fail = r['low'] <= stop if sign == 1 else r['high'] >= stop
        if hit and fail:
            return 'NEUTRAL'
        if hit or fail:
            return 'HIT' if hit else 'FAIL'
    return 'NEUTRAL'


def read(path):
    with open(path) as f:
        return [{k:float(v) for k,v in r.items()} for r in csv.DictReader(f)]


def run(data, cases):
    candles = read(data/'ADA_1h.csv')
    fine = read(data/'ADA_5m.csv')
    fine_by_time = {int(r['openTime']):r for r in fine}
    dataset = []
    for i in range(200, len(candles)-24):
        c = candles[i]
        start = int(c['closeTime'])+1
        times = range(start, start+24*HOUR, 300000)
        if any(t not in fine_by_time for t in times):
            continue
        future = [fine_by_time[t] for t in times]
        # Independent volatility target; matched comparison below uses exact Kotlin target.
        target = max(0.45, features(candles, i)[5]*100*0.55)
        dataset.append((i, start, features(candles, i),
                        [outcome(future, c['close'], target, d) for d in ('BUY','SELL')]))
    if len(dataset) < 200:
        raise ValueError('Insufficient complete history')
    train_end = dataset[int(len(dataset)*0.6)][1]
    test_start = dataset[int(len(dataset)*0.8)][1]
    train = [r for r in dataset if r[1]+24*HOUR < train_end]
    calibration = [r for r in dataset if train_end <= r[1] and r[1]+24*HOUR < test_start]
    test = [r for r in dataset if r[1]>=test_start]
    models = []
    for direction in range(2):
        tr = [r for r in train if r[3][direction] != 'NEUTRAL']
        cal = [r for r in calibration if r[3][direction] != 'NEUTRAL']
        if len(cal)<30 or len({r[3][direction] for r in tr})<2 or len({r[3][direction] for r in cal})<2:
            raise ValueError('Insufficient calibration classes')
        model = GradientBoostingClassifier(n_estimators=40, max_depth=2, learning_rate=0.05, random_state=17)
        model.fit([r[2] for r in tr], [r[3][direction]=='HIT' for r in tr])
        calibrator = LogisticRegression(C=1, random_state=17)
        calibrator.fit(model.decision_function([r[2] for r in cal]).reshape(-1,1),
                       [r[3][direction]=='HIT' for r in cal])
        models.append((model, calibrator))
    def probabilities(x):
        return [float(cal.predict_proba(model.decision_function([x]).reshape(-1,1))[0,1]) for model,cal in models]
    scored=[]
    for r in test:
        probs=probabilities(r[2]); d=int(np.argmax(probs))
        scored.append(dict(time=r[1], direction=('BUY','SELL')[d], probability=probs[d],
                           accepted=probs[d]>=0.65, outcome=r[3][d]))
    matched=[]
    for c in cases:
        t=int(c['signalCloseTime'])+1
        if t<test_start:
            continue
        indices=[i for i,r in enumerate(candles) if int(r['closeTime'])==t-1]
        if not indices:
            continue
        probs=probabilities(features(candles, indices[0])); d=int(np.argmax(probs))
        times=range(t,t+24*HOUR,300000)
        if any(v not in fine_by_time for v in times):
            continue
        result=outcome([fine_by_time[v] for v in times],float(c['entry']),float(c['targetPct']),('BUY','SELL')[d])
        matched.append(dict(time=t, baseline_outcome=c['outcome'], alternative_outcome=result,
                            accepted=probs[d]>=0.65, probability=probs[d]))
    selected=[r for r in scored if r['accepted']]
    resolved=[r for r in selected if r['outcome']!='NEUTRAL']
    return dict(status='EXPLORATORY_ONLY', train_cases=len(train), calibration_cases=len(calibration),
                test_cases=len(test), test_start=test_start, selected=len(selected),
                coverage=len(selected)/len(test), resolved=len(resolved),
                accuracy=sum(r['outcome']=='HIT' for r in resolved)/len(resolved) if resolved else None,
                brier=brier_score_loss([r['outcome']=='HIT' for r in resolved],
                                       [r['probability'] for r in resolved]) if resolved else None,
                predictions=scored, matched_same_targets=matched,
                limitations=['Historical period already inspected; not independent future validation.',
                             'All-candle volatility target differs from Kotlin; only matched cases use identical targets.',
                             'Overlapping horizons; cases are correlated.',
                             'No costs, execution or profit evaluation; no Android integration.',
                             'Regime specialization and prospective frozen-model registry remain pending.'])


if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('--data',required=True); p.add_argument('--cases',required=True); p.add_argument('--output',required=True)
    args=p.parse_args()
    with open(args.cases) as f:
        report=run(Path(args.data),list(csv.DictReader(f)))
    Path(args.output).parent.mkdir(parents=True,exist_ok=True)
    Path(args.output).write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ('predictions','matched_same_targets')},indent=2))
