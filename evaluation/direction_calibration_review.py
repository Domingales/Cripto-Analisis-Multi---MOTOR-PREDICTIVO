"""Read-only BUY/SELL diagnostics on pre-examination data, no model fitting."""
import argparse
import csv
import datetime as dt
import hashlib
import json
import pickle
from pathlib import Path
import numpy as np
import sklearn
from sklearn.metrics import roc_auc_score
from magnitude_candidate import phase, TRAIN_END, CAL_END, TEST_START, DAY, read_labels

MODEL_SHA='781f093ff8b87cb733fab9dfe906b124772ccd7be2afcef5ba09b21782d2a10e'
PHASES=('TRAIN','CALIBRATION','DEVELOPMENT_CHECK')
THRESHOLDS=(.5,.6,.7,.8,.9)
TARGET_BOUNDS=(0,.5,1,2,4,float('inf'))


def target_bin(q):
    if not np.isfinite(q) or q<=0:raise ValueError('Invalid magnitude')
    for lo,hi in zip(TARGET_BOUNDS,TARGET_BOUNDS[1:]):
        if lo<=q<hi:return f'[{lo},{hi})'


def allowed_rows(path):
    rows=[];last=-1
    with Path(path).open() as f:
        for line in f:
            r=json.loads(line);t=int(r['time'])
            if t<=last:raise ValueError('Unordered/duplicate feature rows')
            last=t
            if t>=TEST_START:continue  # Do not validate/use future variables or outcomes.
            actual=phase(t)
            if actual not in PHASES:continue
            if r['phase']!=actual:raise ValueError('Phase differs from purged chronological protocol')
            x=r['x'];q=r['target']
            if len(x)!=46 or not np.isfinite(x).all() or abs(x[-1]-q)>1e-12:
                raise ValueError('Invalid as-of features/target')
            target_bin(q)
            rows.append(r)
    if not rows:raise ValueError('No permitted development data')
    return rows


def uncertainty(times,losses,bias):
    blocks=np.asarray(times,dtype=np.int64)//(14*DAY)
    keys=np.unique(blocks)
    if len(keys)<12:
        return dict(blocks=len(keys),interval=None,reason='FEWER_THAN_12_FOURTEEN_DAY_BLOCKS')
    grouped=np.array([[np.sum(blocks==k),losses[blocks==k].sum(),bias[blocks==k].sum()] for k in keys])
    rng=np.random.default_rng(28)
    w=rng.multinomial(len(keys),[1/len(keys)]*len(keys),size=1000)
    values=w@grouped
    return dict(blocks=len(keys),draws=1000,seed=28,
        brier_percentile95=np.quantile(values[:,1]/values[:,0],[.025,.975]).tolist(),
        bias_percentile95=np.quantile(values[:,2]/values[:,0],[.025,.975]).tolist(),
        limits='Descriptive only; cross-block dependence/drift and fitted-data optimism remain.')


def metrics(items,constant,with_uncertainty=False):
    pending=sum(r['status']=='PENDING' for r in items)
    mature=[r for r in items if r['status']!='PENDING']
    if not mature:return dict(n=0,pending=pending,brier=None,auc=None,bins=[],thresholds=[])
    y=np.array([r['status']=='HIT' for r in mature],dtype=float)
    p=np.array([r['probability'] for r in mature],dtype=float)
    if not np.isfinite(p).all() or np.any((p<0)|(p>1)):
        raise ValueError('Probability outside [0,1]')
    brier=float(np.mean((p-y)**2));base=float(np.mean((constant-y)**2))
    bins=[];ece=0.
    indices=np.minimum((p*10).astype(int),9)
    for i in range(10):
        chosen=indices==i;n=int(chosen.sum())
        mp=float(p[chosen].mean()) if n else None
        observed=float(y[chosen].mean()) if n else None
        if n:ece+=n/len(y)*abs(mp-observed)
        bins.append(dict(low=i/10,high=(i+1)/10,n=n,mean_probability=mp,hit_rate=observed))
    thresholds=[]
    for threshold in THRESHOLDS:
        chosen=p>=threshold;n=int(chosen.sum())
        thresholds.append(dict(threshold=threshold,n=n,coverage=n/len(y),
            hit_rate=float(y[chosen].mean()) if n else None,
            score_only_not_certified=True))
    clipped=np.clip(p,1e-15,1-1e-15)
    times=np.array([r['time'] for r in mature],dtype=np.int64)
    blocks=len(np.unique(times//(14*DAY)))
    result=dict(n=len(y),pending=pending,hit_rate=float(y.mean()),mean_probability=float(p.mean()),
        bias=float(np.mean(p-y)),brier=brier,constant_train_probability=constant,
        constant_brier=base,brier_skill_vs_train_constant=1-brier/base if base else None,
        logloss=float(np.mean(-y*np.log(clipped)-(1-y)*np.log1p(-clipped))),
        auc=float(roc_auc_score(y,p)) if len(set(y))==2 else None,
        probability_quantiles=np.quantile(p,[0,.1,.5,.9,1]).tolist(),
        ece10_descriptive=ece,bins=bins,thresholds=thresholds,blocks14d=blocks,
        insufficient_for_group_inference=len(y)<100 or blocks<12)
    if with_uncertainty:result['uncertainty']=uncertainty(times,(p-y)**2,p-y)
    return result


def joint_outcomes(rows,labels,selected_phase):
    allowed=[r for r in rows if r['phase']==selected_phase and
        all(labels[r['time'],s]['status']!='PENDING' for s in ('BUY','SELL'))]
    both=sum(all(labels[r['time'],s]['status']=='HIT' for s in ('BUY','SELL')) for r in allowed)
    neither=sum(all(labels[r['time'],s]['status']!='HIT' for s in ('BUY','SELL')) for r in allowed)
    return dict(mature_pairs=len(allowed),both_exact=both,neither_exact=neither,
                directions_are_not_complementary=True)


def run(source,output):
    source,output=Path(source),Path(output)
    root=source/'magnitude-output'
    freeze=json.loads((root/'candidate-freeze.json').read_text())
    payload=(root/'candidate.pkl').read_bytes()
    actual=hashlib.sha256(payload).hexdigest()
    if actual!=MODEL_SHA or actual!=freeze['model_sha256']:raise ValueError('Frozen model mismatch')
    if (freeze['train_end'],freeze['calibration_end'],freeze['examination_start'])!=(TRAIN_END,CAL_END,TEST_START):
        raise ValueError('Frozen boundaries changed')
    if freeze['code_sha256']!=hashlib.sha256(Path('evaluation/magnitude_candidate.py').read_bytes()).hexdigest():
        raise ValueError('Candidate code differs from frozen experiment')
    if sklearn.__version__!='1.7.2':raise ValueError('Require original scikit-learn 1.7.2 to deserialize model')
    rows=allowed_rows(root/'features.jsonl')
    labels=read_labels(root/'labels.tsv',before=TEST_START)
    regimes={}
    with (source/'replay-kotlin/kotlin_decisions.csv').open() as f:
        for r in csv.DictReader(f):
            t=int(r['signalCloseTime'])
            if t<TEST_START:regimes[t]=r['regime']
    for r in rows:
        if r['time'] not in regimes or any((r['time'],s) not in labels for s in ('BUY','SELL')):
            raise ValueError('Unpaired regime or label')
        r['regime']=regimes[r['time']]
    # Safe only for this verified repository-owned artifact. Never load arbitrary pickle.
    models=pickle.loads(payload)
    if len(models)!=2:raise ValueError('Expected BUY/SELL model pair')
    X=np.array([r['x'] for r in rows]);scored=[];directions={}
    for side,(model,calibrator) in zip(('BUY','SELL'),models):
        raw=model.predict_proba(X)[:,1]
        calibrated=calibrator.predict_proba(model.decision_function(X).reshape(-1,1))[:,1]
        training=[labels[r['time'],side]['status']=='HIT' for r in rows
                  if r['phase']=='TRAIN' and labels[r['time'],side]['status']!='PENDING']
        if not training:raise ValueError('No training prevalence')
        constant=float(np.mean(training));items=[]
        for r,p0,p1 in zip(rows,raw,calibrated):
            items.append(dict(time=r['time'],phase=r['phase'],regime=r['regime'],target=r['target'],
                direction=side,status=labels[r['time'],side]['status'],
                raw_probability=float(p0),probability=float(p1)))
        scored.extend(items);phases={}
        for name in PHASES:
            group=[r for r in items if r['phase']==name]
            raw_group=[{**r,'probability':r['raw_probability']} for r in group]
            def breakdown(key):
                return {k:metrics([r for r in group if key(r)==k],constant)
                        for k in sorted({key(r) for r in group})}
            phases[name]=dict(calibrated=metrics(group,constant,True),raw=metrics(raw_group,constant,True),
                by_regime=breakdown(lambda r:r['regime']),by_target=breakdown(lambda r:target_bin(r['target'])),
                by_month=breakdown(lambda r:dt.datetime.fromtimestamp(r['time']/1000,dt.timezone.utc).strftime('%Y-%m')))
        directions[side]=dict(calibrator_slope=float(calibrator.coef_[0,0]),
            calibrator_intercept=float(calibrator.intercept_[0]),phases=phases)
    data_digest=hashlib.sha256(json.dumps(scored,sort_keys=True,allow_nan=False).encode()).hexdigest()
    report=dict(status='READ_ONLY_PRE_EXAM_DIRECTION_CALIBRATION_DIAGNOSIS',model_sha256=actual,
        diagnostic_data_sha256=data_digest,permitted_rows=len(rows),exam_labels_used=0,
        examined_period_reused=False,refit_performed=False,threshold_changed=False,production_changed=False,
        joint_outcomes={p:joint_outcomes(rows,labels,p) for p in PHASES},directions=directions,
        versions=dict(numpy=np.__version__,sklearn=sklearn.__version__),
        limits=['TRAIN is fitted-data apparent performance, not generalization.',
            'CALIBRATION was used to fit the frozen calibrator; diagnostics there are optimistic.',
            'DEVELOPMENT_CHECK is later pre-exam data; now development, not a future independent holdout.',
            'Overlapping 120h windows reduce effective sample. No certified 90% or independent probability.',
            'Original confidence is not a calibrated probability; this report diagnoses the separate XRP30m candidate.',
            'HIT exact24h only; approximate/late/fail are not positive exact labels. Pending excluded, never failures.',
            'No selection of new thresholds, regimes or model settings; all subgroup results exploratory.',
            'BUY and SELL magnitude events can both occur; never force probability sum to one.'])
    output.mkdir(parents=True,exist_ok=True)
    (output/'diagnostic-scores-pre-exam.jsonl').write_text(''.join(json.dumps(r,allow_nan=False)+'\n' for r in scored))
    (output/'report.json').write_text(json.dumps(report,indent=2,allow_nan=False)+'\n')
    compact={s:{p:{mode:{k:v['phases'][p][mode].get(k) for k in
        ('n','hit_rate','mean_probability','bias','brier','constant_brier','auc','probability_quantiles','blocks14d')}
        for mode in ('raw','calibrated')} for p in PHASES} for s,v in directions.items()}
    (output/'summary.json').write_text(json.dumps(dict(model_sha256=actual,exam_labels_used=0,
        refit_performed=False,threshold_changed=False,directions=compact,joint_outcomes=report['joint_outcomes']),indent=2)+'\n')
    print('CALIBRATION_RESULT_JSON '+json.dumps(compact))
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--output',required=True)
    a=p.parse_args();run(a.source,a.output)
