"""Frozen 150-combination historical reconstruction, outside Android production."""
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
from expanded_history import INTERVALS, assets
from fixed_replay import HOUR, TRAIN_END, TEST_START, CUT, THRESHOLD, summary


def horizon(tf):
    return (336 if tf == '1d' else 72 if tf == '4h' else 24)*HOUR


def contexts(tf):
    return list(dict.fromkeys(('15m',tf,'4h','1d')))


def partition(t, duration):
    if t+duration < TRAIN_END:
        return 'TRAIN'
    if TRAIN_END <= t and t+duration < TEST_START-duration:
        return 'CALIBRATION'
    return 'EXAMINATION' if t >= TEST_START else 'EMBARGO'


def read_series(root, name, step):
    path=root/(name+'.csv')
    m=json.loads((root/(name+'_manifest.json')).read_text())
    if hashlib.sha256(path.read_bytes()).hexdigest()!=m['csv_sha256']:
        raise ValueError('CSV hash mismatch: '+name)
    a=np.loadtxt(path,delimiter=',',skiprows=1,ndmin=2)
    if a.shape!=(m['count'],7) or not np.isfinite(a).all():
        raise ValueError('Invalid values/shape: '+name)
    if (np.any(np.diff(a[:,0])<=0) or np.any(a[:,0]%step) or
        np.any(a[:,6]!=a[:,0]+step-1) or np.any(a[:,6]>=CUT) or
        np.any(a[:,1:5]<=0) or np.any(a[:,5]<0) or
        np.any(a[:,2]<np.maximum(a[:,1],a[:,4])) or np.any(a[:,3]>np.minimum(a[:,1],a[:,4]))):
        raise ValueError('Invalid times/OHLCV: '+name)
    return a,m


class Features:
    def __init__(self, arrays, steps):
        self.arrays,self.steps,self.cache=arrays,steps,{}

    def at(self,t):
        x=[]
        for key,a in self.arrays.items():
            i=int(np.searchsorted(a[:,6],t,side='right'))
            step=self.steps[key]
            if i<200 or a[i-1,6]!=((t+1)//step)*step-1:
                return None
            w=a[i-200:i]
            if np.any(np.diff(w[:,0])!=step):
                return None
            old=self.cache.get(key)
            if old is None or old[0]!=i:
                c,v=w[:,4],w[:,5]
                value=[float(c[-1]/c[-1-k]-1) for k in (1,4,12,24)]
                value += [float(np.std(np.diff(np.log(c))[-24:])),
                          float(np.mean(w[-14:,2]-w[-14:,3])/c[-1]),
                          float(v[-1]/max(np.mean(v[-20:]),1e-12)),
                          float(c[-1]/np.mean(c[-50:])-1),float(c[-1]/np.mean(c)-1)]
                old=(i,value); self.cache[key]=old
            x.extend(old[1])
        return x


def resolution(fine,t,entry,target,side,duration,cutoff=CUT):
    end=t+duration
    if end>=cutoff:
        return dict(status='PENDING',reason='WINDOW_NOT_EXPIRED')
    start=t+1; count=duration//300000
    i=int(np.searchsorted(fine[:,0],start)); w=fine[i:i+count]
    if (len(w)!=count or not len(w) or w[0,0]!=start or w[-1,6]!=end or
        np.any(np.diff(w[:,0])!=300000)):
        return dict(status='PENDING',reason='MISSING_5M_DATA')
    sell=side=='SELL'
    favorable=((entry-w[:,3]) if sell else (w[:,2]-entry))/entry*100
    adverse=((entry-w[:,2]) if sell else (w[:,3]-entry))/entry*100
    touched=np.flatnonzero((favorable>=target)|(adverse<=-target))
    status,reason,when='NEUTRAL','WINDOW_EXPIRED',end
    if len(touched):
        j=int(touched[0]); hit,fail=favorable[j]>=target,adverse[j]<=-target
        status='NEUTRAL' if hit and fail else 'HIT' if hit else 'FAIL'
        reason='BOTH_TOUCHED_SAME_5M_CANDLE' if hit and fail else 'TARGET_FIRST' if hit else 'STOP_FIRST'
        when=int(w[j,6])
    return dict(status=status,reason=reason,outcome_time=when,
                mfe=float(max(0,max(favorable))),mae=float(min(0,min(adverse))))


def freeze_model(training,duration,output,version,inputs,feature_keys):
    models,counts=[],{}
    for index,side in enumerate(('BUY','SELL')):
        tr=[r for r in training['TRAIN'] if r[2][index]['status'] in ('HIT','FAIL')]
        cal=[r for r in training['CALIBRATION'] if r[2][index]['status'] in ('HIT','FAIL')]
        y=[r[2][index]['status']=='HIT' for r in tr]
        yc=[r[2][index]['status']=='HIT' for r in cal]
        counts[side]=dict(train=len(tr),calibration=len(cal),
            latest_train_label=max((r[0]+duration for r in tr),default=None),
            latest_calibration_label=max((r[0]+duration for r in cal),default=None))
        if len(tr)<100 or len(cal)<100 or len(set(y))<2 or len(set(yc))<2:
            models=[]
            break
        model=GradientBoostingClassifier(n_estimators=40,max_depth=2,learning_rate=.05,random_state=17)
        model.fit([r[1] for r in tr],y)
        calibrator=LogisticRegression(C=1,random_state=17)
        calibrator.fit(model.decision_function([r[1] for r in cal]).reshape(-1,1),yc)
        models.append((model,calibrator))
    blob=pickle.dumps(models) if len(models)==2 else b''
    if blob:
        (output/'frozen_model.pkl').write_bytes(blob)
    manifest=dict(version=version,threshold=THRESHOLD,horizon_ms=duration,
        train_end=TRAIN_END,calibration_label_end=TEST_START-duration,examination_start=TEST_START,
        cutoff=CUT,counts=counts,features=feature_keys,inputs=inputs,
        model_sha256=hashlib.sha256(blob).hexdigest() if blob else None,
        status='FROZEN_BEFORE_EXAMINATION' if blob else 'NOT_EVALUABLE_INSUFFICIENT_PRE2023_TRAINING_OR_CALIBRATION')
    (output/'model_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    return models,manifest


def run(data,kotlin,output,symbol,tf,version):
    output.mkdir(parents=True,exist_ok=True)
    duration=horizon(tf)
    names=[(symbol+'USDT_'+k,INTERVALS[k]) for k in contexts(tf)]
    if symbol!='BTC':
        names.append(('BTCUSDT_4h',INTERVALS['4h']))
    arrays,manifests={},{}
    for name,step in names+[(symbol+'USDT_5m',300000)]:
        arrays[name],manifests[name]=read_series(data,name,step)
    fine=arrays.pop(symbol+'USDT_5m')
    features=Features(arrays,dict(names))
    with (kotlin/'kotlin_decisions.csv').open() as f:
        raw=list(csv.DictReader(f))
    baseline={int(r['signalCloseTime']):r for r in raw}
    if len(raw)!=len(baseline) or list(baseline)!=sorted(baseline):
        raise ValueError('Duplicate/unordered Kotlin decisions')
    with (kotlin/'kotlin_accepted.csv').open() as f:
        accepted={int(r['signalCloseTime']) for r in csv.DictReader(f)}
    if not accepted.issubset(baseline):
        raise ValueError('Accepted time without decision')
    training=dict(TRAIN=[],CALIBRATION=[])
    for t,row in baseline.items():
        phase=partition(t,duration)
        if phase not in training:
            continue
        x=features.at(t)
        if x is not None:
            labels=[resolution(fine,t,float(row['entry']),float(row['targetPct']),side,duration)
                    for side in ('BUY','SELL')]
            training[phase].append((t,x,labels))
    models,freeze=freeze_model(training,duration,output,version,
        {n:m['csv_sha256'] for n,m in manifests.items()},list(arrays))
    del training
    decisions=[]; score_rows=[]; xs=[]
    for t in map(int,arrays[symbol+'USDT_'+tf][:,6]):
        if not TEST_START<=t<CUT:
            continue
        row=baseline.get(t); x=features.at(t) if row else None
        side='SELL' if row and 'SELL' in row['direction'] else 'BUY' if row and 'BUY' in row['direction'] else 'WAIT'
        r=dict(time=t,symbol=symbol,timeframe=tf,version=version,model_sha256=freeze['model_sha256'],
            entry=float(row['entry']) if row else None,target_pct=float(row['targetPct']) if row else None,
            horizon_end=t+duration,variables=x,news='absent_neutral',
            kotlin=dict(direction=side,accepted=t in accepted,context=row,
                rejection=row['rejectionReason'] if row else 'INVALID_OR_INSUFFICIENT_CONTEXT'),
            alternative=dict(direction='WAIT',probability=None,accepted=False,
                rejection='INVALID_OR_INSUFFICIENT_CONTEXT' if x is None else
                    'MODEL_NOT_EVALUABLE' if not models else 'BELOW_FROZEN_THRESHOLD'))
        if models and x is not None:
            score_rows.append(len(decisions)); xs.append(x)
        decisions.append(r)
    if xs:
        X=np.array(xs)
        probs=np.column_stack([cal.predict_proba(model.decision_function(X).reshape(-1,1))[:,1] for model,cal in models])
        for j,i in enumerate(score_rows):
            p=probs[j]; best=int(np.argmax(p))
            decisions[i]['alternative'].update(direction=('BUY','SELL')[best],probability=float(p[best]),
                direction_probabilities=p.tolist(),accepted=bool(p[best]>=THRESHOLD),
                rejection='' if p[best]>=THRESHOLD else 'BELOW_FROZEN_THRESHOLD')
    # Save every decision before any examination labels are computed.
    with (output/'decisions.jsonl').open('w') as journal:
        for r in decisions:
            journal.write(json.dumps(r)+'\n')
    with (output/'outcomes.jsonl').open('w') as journal:
        for r in decisions:
            for e in ('kotlin','alternative'):
                side=r[e]['direction']
                result=resolution(fine,r['time'],r['entry'],r['target_pct'],side,duration) if side!='WAIT' and r['entry'] else dict(status='PENDING',reason='NO_DIRECTION_OR_CONTEXT')
                r[e]['result']=result
            journal.write(json.dumps(dict(time=r['time'],kotlin=r['kotlin']['result'],alternative=r['alternative']['result']))+'\n')
    def summaries(rows):
        return {e:summary(rows,e) for e in ('kotlin','alternative')}
    def group(key):
        buckets={}
        for r in decisions:
            buckets.setdefault(key(r),[]).append(r)
        return {k:summaries(v) for k,v in buckets.items()}
    report=dict(status='EXPLORATORY_CHRONOLOGICAL_RECONSTRUCTION_COMPLETE',symbol=symbol,timeframe=tf,
        freeze=freeze,**summaries(decisions),
        coverage=dict(first_decision=decisions[0]['time'] if decisions else None,
            last_decision=decisions[-1]['time'] if decisions else None,
            decisions_with_context=sum(r['entry'] is not None for r in decisions),
            input_primary_candles=len(arrays[symbol+'USDT_'+tf])),
        by_year=group(lambda r:dt.datetime.fromtimestamp(r['time']/1000,dt.timezone.utc).strftime('%Y')),
        by_regime=group(lambda r:r['kotlin']['context']['regime'] if r['kotlin']['context'] else 'NO_VALID_CONTEXT'),
        limitations=['Reconstruction with code designed after 2023; not independent prospective evidence.',
            'Production MTF set 15m/primary/4h/1d; differs from previous ADA1h experiment.',
            'Original backtest uses a 300-candle calculation; online app can use incremental cache.',
            'Overlapping horizons are dependent; no winner declared across 150 comparisons.',
            'Barrier cost sensitivity is not an execution/profitability simulation.',
            'No historical news; gaps and shorter listings excluded without substitutions.'])
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(symbol,tf,report['kotlin']['signals'],report['alternative']['signals'],freeze['status'],flush=True)
    return report


def aggregate_reports(root,output):
    expected={(s,tf) for s in assets(Path(__file__).resolve().parents[1]) for tf in INTERVALS}
    reports={}
    for p in root.rglob('report.json'):
        r=json.loads(p.read_text()); key=(r['symbol'],r['timeframe'])
        if key not in expected or key in reports:
            raise ValueError('Unexpected or duplicate combination: '+str(key))
        reports[key]=r
    versions={r['freeze'].get('version') for r in reports.values()}
    if len(versions)>1:
        raise ValueError('Mixed engine versions cannot form one experiment')
    input_hashes={}
    for r in reports.values():
        for name,digest in r['freeze'].get('inputs',{}).items():
            if name in input_hashes and input_hashes[name]!=digest:
                raise ValueError('Mixed historical input versions: '+name)
            input_hashes[name]=digest
    missing=sorted(expected-set(reports))
    output.mkdir(parents=True,exist_ok=True)
    result=dict(status='COMPLETE_150_COMBINATIONS' if not missing else 'PARTIAL_NOT_COMPLETE',
        completed=len(reports),expected=len(expected),missing=missing,
        fixed_alternative_evaluable=sum(r['freeze']['status']=='FROZEN_BEFORE_EXAMINATION' for r in reports.values()),
        not_evaluable=[dict(symbol=k[0],timeframe=k[1],reason=r['freeze']['status'],
                           counts=r['freeze'].get('counts',{}))
                       for k,r in sorted(reports.items()) if r['freeze']['status']!='FROZEN_BEFORE_EXAMINATION'],
        reports=[reports[k] for k in sorted(reports)],winner=None)
    (output/'summary_150.json').write_text(json.dumps(result,indent=2)+'\n')
    with (output/'summary_150.csv').open('w',newline='') as f:
        w=csv.writer(f); w.writerow(['symbol','timeframe','engine','model_status','decisions','signals','HIT','FAIL','NEUTRAL','PENDING','accuracy'])
        for k,r in sorted(reports.items()):
            for e in ('kotlin','alternative'):
                m=r[e]; w.writerow([*k,e,r['freeze']['status'],*[m[n] for n in ('decisions','signals','HIT','FAIL','NEUTRAL','PENDING','accuracy')]])
        for k in missing:
            for e in ('kotlin','alternative'):
                w.writerow([*k,e,'NOT_RUN_OR_NO_VALID_REPORT',*(['']*7)])
    print(json.dumps({k:v for k,v in result.items() if k!='reports'}))
    # Bounded per-combination records make the full table independently inspectable in CI logs.
    for report in result['reports']:
        print('RESULT_ROW_JSON '+json.dumps({k:report[k] for k in ('symbol','timeframe','freeze','kotlin','alternative','coverage') if k in report},separators=(',',':')))
    if missing:
        raise ValueError('Incomplete experiment; missing '+str(len(missing))+' combinations')


if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('--matrix',action='store_true'); p.add_argument('--aggregate',type=Path)
    p.add_argument('--data',type=Path); p.add_argument('--kotlin',type=Path); p.add_argument('--output',type=Path)
    p.add_argument('--symbol'); p.add_argument('--timeframe',choices=INTERVALS); p.add_argument('--version')
    a=p.parse_args()
    if a.matrix:
        print(json.dumps(dict(symbol=assets(Path(__file__).resolve().parents[1]),timeframe=list(INTERVALS))))
    elif a.aggregate:
        aggregate_reports(a.aggregate,a.output)
    else:
        if a.symbol not in assets(Path(__file__).resolve().parents[1]):
            p.error('Symbol absent from app catalog')
        run(a.data,a.kotlin,a.output,a.symbol,a.timeframe,a.version)
