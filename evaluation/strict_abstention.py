"""Frozen, pre-exam evidence gate on ORIGINAL signals, no predictor fitting."""
import argparse
import csv
import datetime as dt
import hashlib
import json
import math
from collections import Counter, defaultdict
from pathlib import Path
from statistics import NormalDist
from frozen_engine_digest import engine_digest
from six_year_original_audit import summarize, first_episodes, DAY

TRAIN_END = 1680307200000
EXAM_START = 1698537600000  # 2023-10-29T00:00Z
CUT = 1790953200000
FAMILIES = ('direction','regime','regime_target')


def phase(t):
    if t+5*DAY < TRAIN_END: return 'DEVELOPMENT'
    if TRAIN_END <= t and t+5*DAY < EXAM_START: return 'VALIDATION'
    if EXAM_START <= t < CUT: return 'EXAMINATION'
    return 'EMBARGO'


def key(r, family):
    base = [r['direction']]
    if family != 'direction': base.append(r['regime'])
    if family == 'regime_target':
        q=r['target']
        if not math.isfinite(q) or q<=0: raise ValueError('Invalid target')
        base.append('LT1' if q<1 else '1_TO_2' if q<2 else 'GE2')
    return '|'.join(base)


def lower_bound(hits, n, z):
    if not n: return None
    p=hits/n
    return (p+z*z/(2*n)-z*math.sqrt(p*(1-p)/n+z*z/(4*n*n)))/(1+z*z/n)


def evidence(rows, z):
    mature=[r for r in rows if r['status']!='PENDING']
    # Select first among ALL rows, not first after learning which were pending.
    first=[r for r in first_episodes(rows) if r['status']!='PENDING']
    n=len(mature);h=sum(r['status']=='HIT' for r in mature)
    m=len(first);wins=sum(r['status']=='HIT' for r in first)
    blocks=len({r['time']//(14*DAY) for r in mature})
    reasons=[]
    for ok,reason in [(n>=100,'FEWER_THAN_100_MATURE_SIGNALS'),(blocks>=6,'FEWER_THAN_6_BLOCKS'),
                      (m>=10,'FEWER_THAN_10_MATURE_FIRST_EPISODES'),
                      (n>0 and h/n>=.95,'EXACT_RATE_BELOW_95'),
                      (m>0 and wins/m>=.90,'FIRST_EPISODE_RATE_BELOW_90')]:
        if not ok:reasons.append(reason)
    lower=lower_bound(wins,m,z)
    screened=not reasons
    if lower is None or lower<.90: reasons.append('ADJUSTED_WILSON_LOWER_BELOW_90')
    return dict(**summarize(rows),blocks14d=blocks,first_episodes=summarize(first),
                adjusted_wilson_lower=lower,screen_pass=screened,strict_pass=not reasons,reasons=reasons)


def freeze(rows, labels):
    allowed=[]
    for r in rows:
        p=phase(r['time'])
        if p in ('DEVELOPMENT','VALIDATION'):
            allowed.append(dict(r,status=labels[r['time'],r['direction']],phase=p))
    groups={family:sorted({key(r,family) for r in allowed}) for family in FAMILIES}
    count=sum(map(len,groups.values()))
    z=NormalDist().inv_cdf(1-.05/(2*2*max(1,count)))
    result={}
    for family,keys in groups.items():
        result[family]={}
        for k in keys:
            phases={p:evidence([r for r in allowed if r['phase']==p and key(r,family)==k],z)
                    for p in ('DEVELOPMENT','VALIDATION')}
            result[family][k]=dict(phases=phases,screen_pass=all(v['screen_pass'] for v in phases.values()),
                                  strict_pass=all(v['strict_pass'] for v in phases.values()))
    return dict(train_end=TRAIN_END,examination_start=EXAM_START,groups=count,z=z,
                families=result,production_enabled=False,exam_labels_used=0)


def read_labels(path, before=None):
    labels={}
    with Path(path).open() as f:
        for v in csv.reader(f,delimiter='\t'):
            t=int(v[0])
            if before is not None and t+5*DAY>=before:continue
            k=t,v[1]
            if k in labels or v[2] not in ('HIT','APPROXIMATE','LATE','FAIL','PENDING'):
                raise ValueError('Invalid duplicate label')
            labels[k]=v[2]
    return labels


def run(source, output):
    source,output=Path(source),Path(output)
    root=Path(__file__).resolve().parents[1]
    digest=engine_digest(root)
    if digest!='f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7':
        raise ValueError('Predictor changed')
    manifest=json.loads((source/'magnitude-output/prelabel-manifest.json').read_text())
    criterion=root/'app/src/main/java/com/domingales/criptoanalisis/multi/domain/ForecastCriterion.kt'
    if hashlib.sha256(criterion.read_bytes()).hexdigest()!=manifest['evaluator_sha256']:
        raise ValueError('Evaluator changed')
    with (source/'replay-kotlin/kotlin_accepted.csv').open() as f:
        accepted={int(r['signalCloseTime']) for r in csv.DictReader(f)}
    with (source/'replay-kotlin/kotlin_decisions.csv').open() as f:
        regimes={int(r['signalCloseTime']):r['regime'] for r in csv.DictReader(f)}
    rows=[];last=-1;seen=set()
    with (source/'magnitude-output/features.jsonl').open() as f:
        for line in f:
            r=json.loads(line);t=r['time']
            if t<=last:raise ValueError('Unordered or duplicate decision')
            last=t
            if r['original_accepted']!=(t in accepted):raise ValueError('Acceptance mismatch')
            if not r['original_accepted']:continue
            if r['original_direction']=='WAIT':raise ValueError('Accepted WAIT')
            seen.add(t)
            rows.append(dict(time=t,direction='SELL' if 'SELL' in r['original_direction'] else 'BUY',
                             regime=regimes[t],target=r['target']))
    if seen!=accepted:raise ValueError('Missing accepted feature')
    # Only mature pre-exam labels are materialized during rule development.
    frozen=freeze(rows,read_labels(source/'magnitude-output/labels.tsv',before=EXAM_START))
    output.mkdir(parents=True,exist_ok=True)
    payload=json.dumps(frozen,sort_keys=True,indent=2)+'\n'
    (output/'frozen-filter.json').write_text(payload)
    exam=[r for r in rows if phase(r['time'])=='EXAMINATION']
    predictions=[]
    for r in exam:
        choices={family:frozen['families'][family].get(key(r,family),{}).get('strict_pass',False)
                 for family in FAMILIES}
        predictions.append(dict(r,accepted=choices))
    (output/'predictions-before-exam-labels.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in predictions))
    # No rules may change below this line. Resolve examination only after predictions saved.
    labels=read_labels(source/'magnitude-output/labels.tsv')
    baseline=[dict(r,status=labels[r['time'],r['direction']]) for r in predictions]
    def outcomes(selected):
        return dict(overall=summarize(selected),first_episodes_by_direction=summarize(first_episodes(selected)),
                    by_direction={s:summarize([r for r in selected if r['direction']==s]) for s in ('BUY','SELL')},
                    by_year={str(y):summarize([r for r in selected if
                         dt.datetime.fromtimestamp(r['time']/1000,dt.timezone.utc).year==y]) for y in range(2023,2027)})
    families={family:dict(**outcomes([r for r in baseline if r['accepted'][family]]),
                coverage=sum(r['accepted'][family] for r in baseline)/len(baseline) if baseline else None,
                screen_groups=sum(g['screen_pass'] for g in frozen['families'][family].values()),
                admitted_groups=sum(g['strict_pass'] for g in frozen['families'][family].values()))
              for family in FAMILIES}
    rejection=Counter(reason for groups in frozen['families'].values() for g in groups.values()
                       for phase_result in g['phases'].values() for reason in phase_result['reasons'])
    report=dict(status='EXPLORATORY_STRICT_ABSTENTION_NOT_CERTIFIED',symbol='XRP',timeframe='30m',
                exam_start=EXAM_START,cutoff=CUT,first_archived_decision=1623657599999,
                predictor_sha256=digest,filter_sha256=hashlib.sha256(payload.encode()).hexdigest(),
                original=outcomes(baseline),families=families,development_rejections=dict(rejection),
                frozen_groups=frozen['groups'],production_changed=False,certified_probability=None,
                limits=['No alerts means undefined precision, not100%.',
                        'Previously inspected historical period; not independent prospective evidence.',
                        'Binomial Wilson is descriptive sensitivity: episode independence unproven.',
                        'Original targets/times unchanged; no execution-cost/profit claim.',
                        'Scope XRP30m only;2020-context warmup;archive ends2026-10-02.',
                        'All three fixed families disclosed; no post-exam tuning or winner selection.'])
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print('STRICT_RESULT_JSON '+json.dumps(report))
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--output',required=True)
    a=p.parse_args();run(a.source,a.output)
