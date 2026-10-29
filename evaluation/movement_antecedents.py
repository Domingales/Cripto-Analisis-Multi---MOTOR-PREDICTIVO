"""Fixed-magnitude event study: necessity is not sufficiency or tradability."""
import argparse
import csv
import hashlib
import json
from collections import deque
from pathlib import Path
import numpy as np
from expanded_replay import read_series
from strict_abstention import phase
from frozen_engine_digest import engine_digest

STEP=300000
NAMES=('volume_1_5','adx_25','squeeze20','breakout20','atr_sma_expansion',
       'adx_rising90m','mtf_aligned','btc_momentum4h','momentum4_30m','close_pressure_volume')
EVENTS=((4.,24),(8.,72))


def forward_max(a,n):
    if n<=0:raise ValueError('Invalid window')
    out=np.empty(max(0,len(a)-n+1));q=deque()
    for i,v in enumerate(a):
        while q and q[0]<=i-n:q.popleft()
        while q and a[q[-1]]<=v:q.pop()
        q.append(i)
        if i>=n-1:out[i-n+1]=a[q[0]]
    return out


def conditions(w,r,snapshot,snapshots,side):
    if len(w)!=200 or np.any(np.diff(w[:,0])!=1800000):return [None]*len(NAMES)
    sign=1 if side=='BUY' else -1
    c=w[:,4];vol=w[-1,5];mean=w[-21:-1,5].mean()
    volume=vol>=1.5*mean if mean>0 else None
    prices=np.lib.stride_tricks.sliding_window_view(c,20)
    widths=4*prices.std(axis=1)/prices.mean(axis=1)
    squeeze=widths[-1]<=np.quantile(widths[:-1],.2)
    breakout=c[-1]>w[-21:-1,2].max() if sign==1 else c[-1]<w[-21:-1,3].min()
    tr=np.maximum(w[1:,2]-w[1:,3],np.maximum(abs(w[1:,2]-c[:-1]),abs(w[1:,3]-c[:-1])))
    old_atr=tr[-21:-7].mean()
    atr=tr[-14:].mean()>=1.2*old_atr if old_atr>0 else None
    t=r['time'];previous=[snapshots.get(t-k*1800000) for k in (1,2,3)]
    rising=(float(snapshot['adx'])>=20 and float(snapshot['adx'])>float(previous[-1]['adx'])) if all(previous) else None
    span=w[-1,2]-w[-1,3]
    position=(c[-1]-w[-1,3])/span if span>0 else None
    pressure=None if position is None or volume is None else volume and (position>=.8 if sign==1 else position<=.2)
    return [volume,float(snapshot['adx'])>=25,squeeze,breakout,atr,rising,
            sign*float(snapshot['mtfScore'])>=8,sign*r['x'][36]>0,sign*r['x'][10]>0,pressure]


def label(fine,extrema,t,entry,pct,hours,side):
    n=hours*12;j=int(np.searchsorted(fine[:,0],t+1))
    if (j+n>len(fine) or fine[j,0]!=t+1 or fine[j+n-1,6]!=t+hours*3600000):return None
    # Strictly increasing aligned timestamps + correct endpoints imply no gaps.
    maximum,minimum=extrema
    move=(maximum[j]/entry-1)*100 if side=='BUY' else (1-minimum[j]/entry)*100
    return bool(move>=pct)


def table(rows,name,hours,spaced=False):
    if spaced:
        chosen=[];end=-1
        for r in rows:
            if r['time']>end:chosen.append(r);end=r['time']+hours*3600000
        rows=chosen
    tp=fp=fn=tn=pending=unknown=0;examples=[]
    for r in rows:
        y=r['event'];a=r['conditions'][name]
        if y is None:pending+=1;continue
        if a is None:unknown+=1;continue
        if y and a:tp+=1
        elif y:
            fn+=1
            if len(examples)<3:examples.append(dict(time=r['time'],entry=r['entry']))
        elif a:fp+=1
        else:tn+=1
    n=tp+fp+fn+tn;base=(tp+fn)/n if n else None
    precision=tp/(tp+fp) if tp+fp else None
    return dict(TP=tp,FP=fp,FN=fn,TN=tn,pending=pending,unknown=unknown,
                base_event_rate=base,precision=precision,antecedent_coverage=tp/(tp+fn) if tp+fn else None,
                lift=precision/base if precision is not None and base else None,
                universal_refuted=fn>0,counterexamples=examples,
                limits='Overlapping windows; spaced variant not proven independent.')


def run(data,source,output):
    data,source,output=Path(data),Path(source),Path(output)
    digest=engine_digest(Path(__file__).resolve().parents[1])
    if digest!='f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7':raise ValueError('Predictor drift')
    manifest=json.loads((source/'magnitude-output/prelabel-manifest.json').read_text())
    fine,fm=read_series(data,'XRPUSDT_5m',STEP)
    primary,pm=read_series(data,'XRPUSDT_30m',1800000)
    for name,m in [('XRPUSDT_5m',fm),('XRPUSDT_30m',pm)]:
        if m['csv_sha256']!=manifest['inputs'][name]:raise ValueError('Source lineage mismatch:'+name)
    with (source/'replay-kotlin/kotlin_decisions.csv').open() as f:
        snapshots={int(r['signalCloseTime']):r for r in csv.DictReader(f)}
    rows=[];last=-1
    with (source/'magnitude-output/features.jsonl').open() as f:
        for line in f:
            r=json.loads(line);t=r['time']
            if t<=last:raise ValueError('Unordered decisions')
            last=t;i=int(np.searchsorted(primary[:,6],t,side='right'))
            if i<200 or primary[i-1,6]!=t or not np.isclose(primary[i-1,4],r['entry'],rtol=0,atol=1e-12):raise ValueError('Entry mismatch')
            flags={s:conditions(primary[i-200:i],r,snapshots[t],snapshots,s) for s in ('BUY','SELL')}
            rows.append(dict(time=t,entry=r['entry'],flags=flags,phase=phase(t)))
    output.mkdir(parents=True,exist_ok=True)
    # All antecedents known before materializing future movement outcomes.
    (output/'antecedents-before-events.jsonl').write_text(''.join(json.dumps(r,default=lambda x:bool(x))+'\n' for r in rows))
    report=dict(symbol='XRP',timeframe='30m',events={},production_changed=False,
                inputs={'5m':fm,'30m':pm},predictor_sha256=digest,windows=len(rows),
                limits=['Only XRP30m,archive2020–2026-10-02,common decisions start2021.',
                        'New fixed4%24h/8%72h event,not original magnitude-time prediction metric.',
                        'Antecedents precede future target touch,not necessarily movement onset.',
                        'Necessary and sufficient conditions differ;no universal guarantee.',
                        'OHLCV lacks historical order flow,depth,OI,funding and news/attention.',
                        'Past inspected history,descriptive association,not independent prediction test.'])
    for pct,hours in EVENTS:
        n=hours*12;maximum=forward_max(fine[:,2],n);minimum=-forward_max(-fine[:,3],n)
        event={}
        for side in ('BUY','SELL'):
            resolved=[dict(time=r['time'],entry=r['entry'],phase=r['phase'],
                           conditions=dict(zip(NAMES,r['flags'][side])),
                           event=label(fine,(maximum,minimum),r['time'],r['entry'],pct,hours,side)) for r in rows]
            event[side]={p:{name:dict(all_windows=table([r for r in resolved if r['phase']==p],name,hours),
                                    first_spaced=table([r for r in resolved if r['phase']==p],name,hours,True))
                             for name in NAMES} for p in ('DEVELOPMENT','VALIDATION','EXAMINATION')}
        report['events'][f'{pct:g}pct_{hours}h']=event
    (output/'report.json').write_text(json.dumps(report,indent=2,default=lambda x:bool(x))+'\n')
    compact={event:{side:{name:stats['all_windows'] for name,stats in phases['EXAMINATION'].items()}
                    for side,phases in sides.items()} for event,sides in report['events'].items()}
    (output/'summary.json').write_text(json.dumps(compact,indent=2)+'\n')
    print('ANTECEDENT_RESULT_JSON '+json.dumps(compact))
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser()
    for name in ('data','source','output'):p.add_argument('--'+name,required=True)
    a=p.parse_args();run(a.data,a.source,a.output)
