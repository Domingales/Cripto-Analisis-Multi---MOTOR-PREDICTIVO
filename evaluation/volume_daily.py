"""Two preregistered daily volume signatures; no production integration."""
import argparse
import csv
import datetime as dt
import hashlib
import json
from pathlib import Path
import numpy as np
from expanded_replay import read_series
from execution_costs import net_return, SLIPPAGES, FEE
from selective_replay import first_episodes

DAY=86400000
STEP=300000
START=1696204800000
CUT=1790953200000
RULES={'UD50':{'lookback':50,'threshold':1,'include_current':True},
       'PP10_VOLUME':{'lookback':10,'strict_maximum':True,'exclude_current':True}}
RULE_HASH=hashlib.sha256(json.dumps(RULES,sort_keys=True).encode()).hexdigest()


def signatures(daily,t):
    """Rows are open,OHLCV,close-time; never inspect a candle after t."""
    i=int(np.searchsorted(daily[:,6],t,side='right'))
    flags={};details={}
    for name,count in [('UD50',51),('PP10_VOLUME',12)]:
        w=daily[max(0,i-count):i]
        if (len(w)!=count or w[-1,6]!=t or np.any(np.diff(w[:,0])!=DAY)):
            flags[name]=None;details[name]={'reason':'INSUFFICIENT_OR_GAPPED_DAILY_HISTORY'};continue
        close=w[:,4];vol=w[1:,5];change=np.diff(close)
        if name=='UD50':
            up=float(vol[change>0].sum());down=float(vol[change<0].sum())
            ratio=up/down if down>0 else None
            flags[name]=ratio>1 if ratio is not None else None
            details[name]=dict(up_volume=up,down_volume=down,ratio=ratio,
                               reason='EVALUABLE' if ratio is not None else 'NO_DOWN_VOLUME')
        else:
            downs=vol[:-1][change[:-1]<0]
            maximum=float(downs.max()) if len(downs) else 0
            flags[name]=bool(change[-1]>0 and vol[-1]>maximum) if maximum>0 else None
            details[name]=dict(prior_down_max=maximum,current_volume=float(vol[-1]),
                               reason='EVALUABLE' if maximum>0 else 'NO_PRIOR_DOWN_VOLUME')
    return flags,details


def strategies(r):
    d=r['original_direction']
    side='SELL' if 'SELL' in d else 'BUY' if 'BUY' in d else None
    out={'BUY_ALL':'BUY'}
    if r['original_accepted'] and side:
        out['ORIGINAL_ALL']=side
        if side=='BUY':out['ORIGINAL_BUY']='BUY'
    for name,accepted in r['flags'].items():
        if accepted:
            out[name]='BUY'
            if r['original_accepted'] and side=='BUY':out['ORIGINAL_BUY_AND_'+name]='BUY'
    return out


def prepare(data,kotlin,output,symbol):
    data,kotlin,output=Path(data),Path(kotlin),Path(output)
    output.mkdir(parents=True,exist_ok=True)
    daily,m=read_series(data,symbol+'USDT_1d',DAY)
    with (kotlin/'magnitude_targets.csv').open() as f:
        raw=list(csv.DictReader(f));targets={int(r['time']):r for r in raw}
    if len(raw)!=len(targets):raise ValueError('Duplicate forecast ranges')
    with (kotlin/'kotlin_accepted.csv').open() as f:
        accepted={int(r['signalCloseTime']) for r in csv.DictReader(f)}
    rows=[];excluded=0;last=-1
    with (kotlin/'kotlin_decisions.csv').open() as f:
        for r in csv.DictReader(f):
            t=int(r['signalCloseTime'])
            if t<=last:raise ValueError('Unordered decisions')
            last=t
            if not START<=t<CUT:continue
            c=targets.get(t)
            if not c or float(c['forecastLowPct'])<=0:
                excluded+=1;continue
            low,high=float(c['forecastLowPct']),float(c['forecastHighPct'])
            if not all(np.isfinite(v) and v>0 for v in (low,high)) or high<low:
                raise ValueError('Invalid original forecast range')
            flags,details=signatures(daily,t)
            j=int(np.searchsorted(daily[:,6],t))
            if j>=len(daily) or daily[j,6]!=t or not np.isclose(daily[j,4],float(r['entry']),rtol=0,atol=1e-12):
                raise ValueError('Original entry differs from daily evidence')
            rows.append(dict(id=f'{symbol}-1d-{t}',time=t,horizon_end=t+5*DAY,symbol=symbol,
                entry=float(r['entry']),target=(low+high)/2,flags=flags,details=details,
                original_direction=r['direction'],original_accepted=t in accepted,regime=r['regime']))
    if not rows:raise ValueError('No evaluable decisions')
    (output/'decisions-before-labels.jsonl').write_text(''.join(json.dumps(r,allow_nan=False)+'\n' for r in rows))
    with (output/'contracts.tsv').open('w') as f:
        for r in rows:
            for side in ('BUY','SELL'):
                f.write(f"{r['id']}\t{symbol}\t{r['time']}\t{r['entry']}\t{side}\t{r['target']}\n")
    evaluator=Path('app/src/main/java/com/domingales/criptoanalisis/multi/domain/ForecastCriterion.kt')
    meta=dict(symbol=symbol,rules=RULES,rule_sha256=RULE_HASH,daily_sha256=m['csv_sha256'],
        code_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        evaluator_sha256=hashlib.sha256(evaluator.read_bytes()).hexdigest(),
        excluded_ranges=excluded,decisions=len(rows),
        daily_opportunities=int(sum(START<=t<CUT for t in daily[:,6])))
    (output/'manifest.json').write_text(json.dumps(meta,indent=2)+'\n')
    print(json.dumps(meta))


def historical_cost(r,side,fine,cutoff=CUT):
    """Historical reconstruction: no invented prospective durability receipts."""
    t,end=r['time'],r['horizon_end'];first=t+1
    if end>=cutoff:return dict(status='PENDING',reason='WINDOW_NOT_CLOSED')
    i=int(np.searchsorted(fine[:,0],first));w=fine[i:i+(end-first+1)//STEP]
    if (not len(w) or w[0,0]!=first or w[-1,6]!=end or np.any(np.diff(w[:,0])!=STEP)):
        return dict(status='PENDING',reason='MISSING_OFFICIAL_5M')
    reference,q=r['entry'],r['target']/100
    lower,upper=reference*(1-q),reference*(1+q);entry=float(w[0,1])
    if not lower<entry<upper:return dict(status='NOT_EXECUTABLE')
    target,stop=(upper,lower) if side=='BUY' else (lower,upper)
    exits=[float(w[-1,4])];reason='HORIZON_CLOSE';when=int(w[-1,6])
    for b in w:
        op,hi,lo=map(float,b[1:4])
        stop_gap=op<=stop if side=='BUY' else op>=stop
        target_gap=op>=target if side=='BUY' else op<=target
        stop_hit=lo<=stop if side=='BUY' else hi>=stop
        target_hit=hi>=target if side=='BUY' else lo<=target
        if stop_gap:exits=[op];reason='STOP_GAP';when=int(b[0])
        elif target_gap:exits=[target];reason='TARGET_GAP_CAPPED';when=int(b[0])
        elif stop_hit and target_hit:exits=[stop,target];reason='AMBIGUOUS_BOTH_TOUCHED';when=int(b[6])
        elif stop_hit or target_hit:exits=[stop if stop_hit else target];reason='STOP' if stop_hit else 'TARGET';when=int(b[6])
        else:continue
        break
    scenarios=[]
    for slip in SLIPPAGES:
        values=sorted(net_return(entry,p,side,slip) for p in exits)
        scenarios.append(dict(slippage_per_side=slip,fee_per_side=FEE,return_low=values[0],return_high=values[-1]))
    return dict(status='AMBIGUOUS' if len(exits)>1 else 'SIMULATED',reason=reason,entry=entry,
        exit_time=when,exit_prices=exits,scenarios=scenarios,
        window_sha256=hashlib.sha256(w.astype('<f8').tobytes()).hexdigest())


def metrics(chosen,labels):
    values=[labels[r['id'],side] for r,side in chosen]
    counts={s:sum(v['status']==s for v in values) for s in ('HIT','APPROXIMATE','LATE','FAIL','PENDING')}
    n=len(values)-counts['PENDING']
    return dict(signals=len(values),**counts,exact_rate=counts['HIT']/n if n else None,
        exact_or_approximate_rate=(counts['HIT']+counts['APPROXIMATE'])/n if n else None,
        mean_mfe=float(np.mean([v['mfe'] for v in values])) if values else None,
        mean_mae=float(np.mean([v['mae'] for v in values])) if values else None)


def finish(data,output,symbol):
    data,output=Path(data),Path(output)
    meta=json.loads((output/'manifest.json').read_text())
    if meta['rule_sha256']!=RULE_HASH:raise ValueError('Rules changed after decisions')
    rows=[json.loads(s) for s in (output/'decisions-before-labels.jsonl').read_text().splitlines()]
    labels={}
    with (output/'labels.tsv').open() as f:
        for v in csv.reader(f,delimiter='\t'):
            key=v[0],v[1]
            if key in labels or v[2] not in ('HIT','APPROXIMATE','LATE','FAIL','PENDING'):
                raise ValueError('Duplicate/invalid result')
            labels[key]=dict(status=v[2],reason=v[3],mfe=float(v[4]),mae=float(v[5]))
    if set(labels)!={(r['id'],s) for r in rows for s in ('BUY','SELL')}:
        raise ValueError('Unpaired Kotlin labels')
    fine,fm=read_series(data,symbol+'USDT_5m',STEP)
    names=['BUY_ALL','ORIGINAL_BUY','ORIGINAL_ALL','UD50','PP10_VOLUME',
           'ORIGINAL_BUY_AND_UD50','ORIGINAL_BUY_AND_PP10_VOLUME']
    report={};costrows=[]
    for name in names:
        chosen=[(r,strategies(r)[name]) for r in rows if name in strategies(r)]
        first=set(first_episodes([r for r,_ in chosen]))
        cost=[];locked=-1
        for r,side in chosen:
            # Admission uses fixed known window; no outcomes or early-exit hindsight.
            if r['time']+1<=locked:
                cost.append(dict(id=r['id'],strategy=name,status='OMITTED_ACTIVE_WINDOW'));continue
            locked=r['horizon_end']
            cost.append(dict(id=r['id'],strategy=name,direction=side,**historical_cost(r,side,fine)))
        costrows.extend(cost)
        cs={s:sum(c['status']==s for c in cost) for s in ('SIMULATED','AMBIGUOUS','PENDING','NOT_EXECUTABLE','OMITTED_ACTIVE_WINDOW')}
        resolved=[c for c in cost if c['status'] in ('SIMULATED','AMBIGUOUS')]
        principal=[c['scenarios'][1] for c in resolved]
        report[name]=dict(**metrics(chosen,labels),coverage=len(chosen)/len(rows),
            first_transitive_episode=metrics([(r,s) for r,s in chosen if r['time'] in first],labels),
            by_year={y:metrics([(r,s) for r,s in chosen if dt.datetime.fromtimestamp(r['time']/1000,dt.timezone.utc).strftime('%Y')==y],labels) for y in ('2023','2024','2025','2026')},
            by_regime={g:metrics([(r,s) for r,s in chosen if r['regime']==g],labels) for g in sorted({r['regime'] for r,_ in chosen})},
            execution=dict(counts=cs,mean_return_low=float(np.mean([p['return_low'] for p in principal])) if principal else None,
                mean_return_high=float(np.mean([p['return_high'] for p in principal])) if principal else None,
                definitely_positive=sum(p['return_low']>0 for p in principal),
                definitely_negative=sum(p['return_high']<0 for p in principal),
                scope='Mean per admitted fixed-notional case, not portfolio return or independent evidence'))
    (output/'execution-cases.jsonl').write_text(''.join(json.dumps(c)+'\n' for c in costrows))
    final=dict(status='EXPLORATORY_VOLUME_DAILY_NOT_CERTIFIED',symbol=symbol,manifest=meta,
        fine_sha256=fm['csv_sha256'],decisions=len(rows),
        not_evaluable={n:sum(r['flags'][n] is None for r in rows) for n in RULES},
        target_range_pct=[min(r['target'] for r in rows),max(r['target'] for r in rows)],
        strategies=report,winner=None,certified_probability=None,production_changed=False,
        limits=['Two primary BUY volume signatures, three prespecified assets: six comparisons; no winner from raw maxima.',
            'UD50 is not capital inflow. PP10_VOLUME is not the full pocket pivot method or institutional detection.',
            'Original range and targets unchanged. Exact/approximate/late/fail separate, not operational V2.',
            'Historical code reconstruction with neutral absent news; previously inspected history is exploratory.',
            'Transitive episodes and nonoverlap cost admissions do not establish independence.',
            'Costs hypothetical; symmetrical stop is not original invalidation. No leverage; shorts lack borrowing costs.',
            'No probability score produced: calibration/Brier not applicable. No 90% claim.'])
    (output/'report.json').write_text(json.dumps(final,indent=2,allow_nan=False)+'\n')
    compact={n:{k:report[n][k] for k in ('signals','HIT','APPROXIMATE','LATE','FAIL','PENDING','exact_rate','coverage','execution')} for n in names}
    print('VOLUME_RESULT_JSON '+json.dumps(dict(symbol=symbol,decisions=len(rows),not_evaluable=final['not_evaluable'],strategies=compact)))
    return final


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['prepare','finish'])
    p.add_argument('--data',required=True);p.add_argument('--kotlin');p.add_argument('--output',required=True)
    p.add_argument('--symbol',choices=['BTC','ETH','XRP'],required=True);a=p.parse_args()
    if a.action=='prepare':prepare(a.data,a.kotlin,a.output,a.symbol)
    else:finish(a.data,a.output,a.symbol)
