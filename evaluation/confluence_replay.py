"""Finite conjunction discovery on past-only market flags, not future winners."""
import argparse
import csv
import hashlib
import itertools
import json
import math
from pathlib import Path
from statistics import NormalDist
import numpy as np
from frozen_engine_digest import engine_digest
from strict_abstention import phase, read_labels, EXAM_START, CUT, lower_bound, DAY
from six_year_original_audit import summarize, first_episodes

NAMES=('volume_1_5','adx_25','mtf_8','ma50_aligned','momentum4_aligned',
       'rsi_directional','regime_aligned','atr_moderate')
COMBINATIONS=tuple(itertools.chain(itertools.combinations(range(8),2),itertools.combinations(range(8),3)))


def flags(r, side):
    sign=1 if side=='BUY' else -1
    x=r['x'];s=r['snapshot'];rsi=float(s['rsi'])
    if len(x)!=46 or not all(math.isfinite(float(v)) for v in x):raise ValueError('Invalid features')
    return (float(s['volume'])>=1.5,float(s['adx'])>=25,sign*float(s['mtf'])>=8,
            sign*x[16]>0,sign*x[10]>0,(50<=rsi<=70) if side=='BUY' else (30<=rsi<=50),
            s['regime']==('TREND_UP' if side=='BUY' else 'TREND_DOWN'),.5<=float(s['atr'])<=2)


def spaced(rows):
    chosen=[];end=-1
    for r in rows:
        if r['time']>end:
            chosen.append(r);end=r['time']+5*DAY
    return chosen


def stats(rows):
    first=[r for r in spaced(rows) if r['status']!='PENDING']
    n=len(first);h=sum(r['status']=='HIT' for r in first)
    return dict(**summarize(rows),blocks14d=len({r['time']//(14*DAY) for r in rows if r['status']!='PENDING'}),
                first_spaced=summarize(first),first_transitive=summarize(first_episodes(rows)),
                wilson95_first_spaced=lower_bound(h,n,1.959963984540054),
                wilson_multiple_first_spaced=lower_bound(h,n,NormalDist().inv_cdf(1-.05/(2*168))))


def choose(training, baseline):
    eligible=[r for r in training if r['train']['resolved']>=200 and r['train']['blocks14d']>=6
              and r['train']['first_spaced']['signals']>=10
              and r['train']['exact_rate']>=baseline+.03]
    return sorted(eligible,key=lambda r:(-r['train']['wilson95_first_spaced'],
                -r['train']['exact_rate'],-r['train']['resolved'],r['id']))[:3]


def validate(result, baseline):
    return (result['resolved']>=100 and result['blocks14d']>=4
            and result['first_spaced']['signals']>=5 and result['exact_rate']>=.90
            and result['exact_rate']>=baseline+.03)


def rows_for(rows, labels, side):
    return [dict(time=r['time'],direction=side,status=labels[r['time'],side]) for r in rows]


def run(source, output):
    source,output=Path(source),Path(output);root=Path(__file__).resolve().parents[1]
    digest=engine_digest(root)
    if digest!='f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7':raise ValueError('Predictor drift')
    manifest=json.loads((source/'magnitude-output/prelabel-manifest.json').read_text())
    if hashlib.sha256((root/'app/src/main/java/com/domingales/criptoanalisis/multi/domain/ForecastCriterion.kt').read_bytes()).hexdigest()!=manifest['evaluator_sha256']:
        raise ValueError('Criterion drift')
    with (source/'replay-kotlin/kotlin_decisions.csv').open() as f:
        snapshots={int(r['signalCloseTime']):dict(rsi=r['rsi'],volume=r['volumeRatio'],adx=r['adx'],mtf=r['mtfScore'],regime=r['regime'],atr=r['atrPct']) for r in csv.DictReader(f)}
    rows=[];last=-1
    with (source/'magnitude-output/features.jsonl').open() as f:
        for line in f:
            r=json.loads(line)
            if r['time']<=last:raise ValueError('Unordered decisions')
            last=r['time'];r['snapshot']=snapshots[r['time']];rows.append(r)
    flags_by_side={s:np.array([flags(r,s) for r in rows],dtype=bool) for s in ('BUY','SELL')}
    phases=np.array([phase(r['time']) for r in rows]);indices={p:np.flatnonzero(phases==p) for p in ('DEVELOPMENT','VALIDATION','EXAMINATION')}
    pre=read_labels(source/'magnitude-output/labels.tsv',before=EXAM_START)
    selected=[];search=[];reference={}
    for side in ('BUY','SELL'):
        reference[side]={p:stats(rows_for([rows[i] for i in indices[p]],pre,side)) for p in ('DEVELOPMENT','VALIDATION')}
        training=[]
        for combo in COMBINATIONS:
            mask=flags_by_side[side][:,combo].all(axis=1)
            take=[rows[i] for i in indices['DEVELOPMENT'] if mask[i]]
            train=stats(rows_for(take,pre,side))
            base=reference[side]['DEVELOPMENT'];n=base['resolved']
            hits=train['HIT'];allhits=base['HIT']
            rule=dict(id=side+':'+'+'.join(NAMES[i] for i in combo),side=side,conditions=list(combo),train=train,
                development_contingency=dict(true_positive=hits,false_positive=train['resolved']-hits,
                    false_negative=allhits-hits,true_negative=n-allhits-(train['resolved']-hits)))
            training.append(rule)
        top=choose(training,reference[side]['DEVELOPMENT']['exact_rate'])
        for r in top:
            mask=flags_by_side[side][:,r['conditions']].all(axis=1)
            val=stats(rows_for([rows[i] for i in indices['VALIDATION'] if mask[i]],pre,side))
            selected.append(dict(r,validation=val,admitted=validate(val,reference[side]['VALIDATION']['exact_rate'])))
        search.extend(training)
    frozen=dict(exam_start=EXAM_START,selected=selected,reference_pre_exam=reference,
                search_count=len(search),feature_names=NAMES,exam_labels_used=0,production_enabled=False)
    output.mkdir(parents=True,exist_ok=True)
    payload=json.dumps(frozen,sort_keys=True,indent=2)+'\n';(output/'frozen-rules.json').write_text(payload)
    (output/'development-search.json').write_text(json.dumps(search,indent=2)+'\n')
    predictions=[];conflicts=0
    for i in indices['EXAMINATION']:
        sides=[s for s in ('BUY','SELL') if any(r['admitted'] and r['side']==s and
                flags_by_side[s][i,r['conditions']].all() for r in selected)]
        if len(sides)==2:conflicts+=1
        predictions.append(dict(time=rows[i]['time'],direction=sides[0] if len(sides)==1 else 'WAIT'))
    (output/'predictions-before-exam-labels.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in predictions))
    labels=read_labels(source/'magnitude-output/labels.tsv')
    candidate=[dict(r,status=labels[r['time'],r['direction']]) for r in predictions if r['direction']!='WAIT']
    examrows=[rows[i] for i in indices['EXAMINATION']]
    original=[dict(time=r['time'],direction='SELL' if 'SELL' in r['original_direction'] else 'BUY',
              status=labels[r['time'],'SELL' if 'SELL' in r['original_direction'] else 'BUY'])
              for r in examrows if r['original_accepted']]
    tested=[]
    for rule in selected:
        mask=flags_by_side[rule['side']][:,rule['conditions']].all(axis=1)
        test=stats(rows_for([rows[i] for i in indices['EXAMINATION'] if mask[i]],labels,rule['side']))
        tested.append(dict(rule,examination=test))
    reference_exam={s:stats(rows_for(examrows,labels,s)) for s in ('BUY','SELL')}
    report=dict(status='EXPLORATORY_CONFLUENCE_NOT_CERTIFIED',symbol='XRP',timeframe='30m',
                exam_start=EXAM_START,cutoff=CUT,search_count=len(search),selected=tested,
                candidate=stats(candidate),candidate_by_direction={s:stats([r for r in candidate if r['direction']==s]) for s in ('BUY','SELL')},
                original=stats(original),all_windows_reference=reference_exam,
                evaluable_exam_windows=len(examrows),coverage=len(candidate)/len(examrows),conflicts=conflicts,
                frozen_sha256=hashlib.sha256(payload.encode()).hexdigest(),production_changed=False,
                certified_probability=None,
                limits=['Both directions can hit the magnitude target; events are not complementary.',
                        'All windows searched, not only original alerts; selection frequencies differ.',
                        'Wilson and spaced/transitive episodes do not prove independence.',
                        'Eight fixed flags; no measured SR breakout, ATR expansion or ADX slope in this source.',
                        'Not independent holdout: historical method selection and previous inspection.',
                        'No future outcomes loaded until rule/prediction persistence; no post-exam tuning.',
                        '2020-context, first evaluable2021, archive cutoff2026-10-02;XRP30m only.',
                        'No costs/execution/profitability or90/100%guarantee.'])
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print('CONFLUENCE_RESULT_JSON '+json.dumps(report))
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--output',required=True)
    a=p.parse_args();run(a.source,a.output)
