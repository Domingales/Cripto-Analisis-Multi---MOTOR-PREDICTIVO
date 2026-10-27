"""New magnitude/plazo candidate, chronologically separated; never production."""
import argparse
import csv
import hashlib
import json
import pickle
from pathlib import Path
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import brier_score_loss
from expanded_replay import Features, read_series
from selective_replay import first_episodes

DAY=86400000
TRAIN_END=1680307200000  # 2023-04-01
CAL_END=1688169600000    # 2023-07-01
TEST_START=1696204800000 # 2023-10-02
CUT=1790953200000
THRESHOLD=.90


def phase(t):
    # Conservatively purge full 120h outcome windows, including negative labels.
    end=t+5*DAY
    if end<TRAIN_END: return 'TRAIN'
    if TRAIN_END<=t and end<CAL_END: return 'CALIBRATION'
    if CAL_END<=t and end<TEST_START: return 'DEVELOPMENT_CHECK'
    if TEST_START<=t<CUT: return 'EXAMINATION'
    return 'EMBARGO'


def target(low, high):
    if not all(np.isfinite(x) and x>0 for x in (low,high)) or high<low:
        raise ValueError('Invalid original forecast range')
    return (low+high)/2


def prepare(data, kotlin, output):
    data,kotlin,output=Path(data),Path(kotlin),Path(output)
    output.mkdir(parents=True,exist_ok=True)
    names=[('XRPUSDT_'+tf,step) for tf,step in [('15m',900000),('30m',1800000),('4h',14400000),('1d',DAY)]]+[('BTCUSDT_4h',14400000)]
    arrays,inputs={},{}
    for name,step in names:
        arrays[name],m=read_series(data,name,step); inputs[name]=m['csv_sha256']
    # Validate 5m evidence now, before labelling in Kotlin.
    _,m=read_series(data,'XRPUSDT_5m',300000);inputs['XRPUSDT_5m']=m['csv_sha256']
    features=Features(arrays,dict(names))
    with (kotlin/'magnitude_targets.csv').open() as f:
        targets={int(r['time']):r for r in csv.DictReader(f)}
    with (kotlin/'kotlin_accepted.csv').open() as f:
        accepted={int(r['signalCloseTime']) for r in csv.DictReader(f)}
    rows=[];excluded=0;last=-1
    with (kotlin/'kotlin_decisions.csv').open() as f:
        for r in csv.DictReader(f):
            t=int(r['signalCloseTime'])
            if t<=last: raise ValueError('Unordered/duplicate decisions')
            last=t; x=features.at(t); c=targets.get(t)
            if x is None or c is None or float(c['forecastLowPct'])<=0:
                excluded+=1;continue
            q=target(float(c['forecastLowPct']),float(c['forecastHighPct']))
            if not np.isclose(float(c['entry']),float(r['entry']),rtol=0,atol=1e-12):
                raise ValueError('Entry mismatch')
            rows.append(dict(time=t,entry=float(r['entry']),target=q,x=x+[q],
                phase=phase(t),original_direction=r['direction'],original_accepted=t in accepted))
    if not rows: raise ValueError('No evaluable rows')
    with (output/'features.jsonl').open('w') as f, (output/'contracts.tsv').open('w') as c:
        for r in rows:
            f.write(json.dumps(r,allow_nan=False)+'\n')
            for side in ('BUY','SELL'):
                c.write(f"{r['time']}\t{r['entry']}\t{side}\t{r['target']}\n")
    meta=dict(inputs=inputs,excluded=excluded,rows=len(rows),
        boundaries=dict(train_end=TRAIN_END,calibration_end=CAL_END,examination_start=TEST_START),
        evaluator_sha256=hashlib.sha256(Path('app/src/main/java/com/domingales/criptoanalisis/multi/domain/ForecastCriterion.kt').read_bytes()).hexdigest())
    (output/'prelabel-manifest.json').write_text(json.dumps(meta,indent=2)+'\n')
    print(json.dumps(meta))


def read_labels(path, before=None):
    out={}
    with Path(path).open() as f:
        for v in csv.reader(f,delimiter='\t'):
            t=int(v[0])
            if before is not None and t>=before: continue
            if (t,v[1]) in out: raise ValueError('Duplicate label')
            if v[2] not in ('HIT','APPROXIMATE','LATE','FAIL','PENDING'):
                raise ValueError('Invalid label')
            out[t,v[1]]=dict(status=v[2],mfe=float(v[4]),mae=float(v[5]))
    return out


def summary(rows, labels):
    values=[labels[r['time'],r['direction']] for r in rows]
    counts={s:sum(v['status']==s for v in values) for s in ('HIT','APPROXIMATE','LATE','FAIL','PENDING')}
    mature=len(values)-counts['PENDING']
    return dict(signals=len(rows),**counts,exact_rate=counts['HIT']/mature if mature else None)


def fit_evaluate(output):
    output=Path(output)
    rows=[json.loads(s) for s in (output/'features.jsonl').read_text().splitlines()]
    # Examination labels are not materialized until model and predictions are saved.
    labels=read_labels(output/'labels.tsv',before=TEST_START)
    models=[];counts={}
    for side in ('BUY','SELL'):
        groups={p:[r for r in rows if r['phase']==p and labels[r['time'],side]['status']!='PENDING']
                for p in ('TRAIN','CALIBRATION')}
        counts[side]={p:len(v) for p,v in groups.items()}
        if any(len(v)<100 for v in groups.values()): raise ValueError('Insufficient training/calibration')
        tr,cal=groups['TRAIN'],groups['CALIBRATION']
        y=lambda a:[labels[r['time'],side]['status']=='HIT' for r in a]
        if len(set(y(tr)))<2 or len(set(y(cal)))<2: raise ValueError('Single class')
        model=GradientBoostingClassifier(n_estimators=40,max_depth=2,learning_rate=.05,random_state=17)
        model.fit([r['x'] for r in tr],y(tr))
        calibrator=LogisticRegression(C=1,random_state=17)
        calibrator.fit(model.decision_function([r['x'] for r in cal]).reshape(-1,1),y(cal))
        models.append((model,calibrator))
    blob=pickle.dumps(models);(output/'candidate.pkl').write_bytes(blob)
    frozen=dict(model_sha256=hashlib.sha256(blob).hexdigest(),threshold=THRESHOLD,counts=counts,
        objective='EXACT_HIT_MAGNITUDE_TIME_V1',features='45 market-only as-of features plus shared original target',
        train_end=TRAIN_END,calibration_end=CAL_END,examination_start=TEST_START,
        production_enabled=False,code_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
    (output/'candidate-freeze.json').write_text(json.dumps(frozen,indent=2)+'\n')
    examine=[r for r in rows if r['phase']=='EXAMINATION']
    scores=np.column_stack([c.predict_proba(m.decision_function([r['x'] for r in examine]).reshape(-1,1))[:,1] for m,c in models])
    predictions=[]
    for r,ps in zip(examine,scores):
        index=int(np.argmax(ps))
        predictions.append(dict(time=r['time'],horizon_end=r['time']+5*DAY,
            direction=('BUY','SELL')[index],probability=float(ps[index]),
            probabilities=ps.tolist(),accepted=bool(ps[index]>=THRESHOLD),target=r['target']))
    (output/'candidate-decisions-before-exam-labels.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in predictions))
    labels=read_labels(output/'labels.tsv')
    chosen=[r for r in predictions if r['accepted']]
    baseline=[dict(time=r['time'],direction='SELL' if 'SELL' in r['original_direction'] else 'BUY')
        for r in examine if r['original_accepted'] and r['original_direction']!='WAIT']
    quality={}
    for i,side in enumerate(('BUY','SELL')):
        mask=[j for j,r in enumerate(examine) if labels[r['time'],side]['status']!='PENDING']
        quality[side]=dict(n=len(mask),brier=float(brier_score_loss(
            [labels[examine[j]['time'],side]['status']=='HIT' for j in mask],scores[mask,i]))) if mask else dict(n=0,brier=None)
    first=set(first_episodes(chosen))
    report=dict(status='EXPLORATORY_NEW_MAGNITUDE_CANDIDATE_NOT_CERTIFIED',decisions=len(examine),
        candidate=summary(chosen,labels),baseline=summary(baseline,labels),
        candidate_first_transitive_episode=summary([r for r in chosen if r['time'] in first],labels),
        coverage=len(chosen)/len(examine),max_estimated_probability=float(scores.max()),
        probability_quality_all_directions=quality,freeze=frozen,
        target_range_pct=[min(r['target'] for r in examine),max(r['target'] for r in examine)],
        first_examination=examine[0]['time'],last_examination=examine[-1]['time'],
        certified_probability=None,winner=None,production_changed=False,
        limits=['Original range midpoint, identical target and entry for both engines; different directions possible.',
            'Exact HIT only is positive training label; APPROXIMATE is separately reported.',
            'Code and method designed after inspecting this historical period: exploratory, not pristine holdout.',
            'Archived official history; absent historical news remain neutral.',
            'Overlapping labels and episodes are dependent; no binomial certification.',
            'No execution/cost simulation; a magnitude HIT can still lose at a stop.',
            'No retraining or replacement of the active prospective models.'])
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['prepare','fit'])
    p.add_argument('--data');p.add_argument('--kotlin');p.add_argument('--output',required=True)
    a=p.parse_args()
    if a.action=='prepare':prepare(a.data,a.kotlin,a.output)
    else:fit_evaluate(a.output)
