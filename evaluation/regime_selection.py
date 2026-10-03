"""Frozen exploratory filter; separate shadow ledger, no Android integration."""
import argparse, csv, datetime as dt, hashlib, io, json, math, statistics, time, zipfile
from pathlib import Path

HOUR = 3600000
RULE = {'id':'trend-volume-btc-v1', 'ada_4h_distance_200':0.01,
        'volume_ratio_1h_min':1.0,
        'minimum_resolved_for_review':100,
        'description':'Existing accepted signal, ADA trend aligned with BTC, 1h volume >=20-candle mean.'}
RULE_HASH = hashlib.sha256(json.dumps(RULE, sort_keys=True).encode()).hexdigest()


def classify(x):
    if x is None or len(x) != 36 or not all(math.isfinite(v) for v in x):
        return 'INVALID'
    a50, a200 = x[16], x[17]
    if a50 > 0 and a200 >= RULE['ada_4h_distance_200']:
        return 'TREND_UP'
    if a50 < 0 and a200 <= -RULE['ada_4h_distance_200']:
        return 'TREND_DOWN'
    return 'RANGE_OR_MIXED'


def selection(x, direction, base_accepted):
    regime = classify(x)
    if regime == 'INVALID':
        return False, 'INVALID_CONTEXT'
    if not base_accepted:
        return False, 'BASE_NO_SIGNAL'
    if direction not in ('BUY','SELL'):
        return False, 'NO_DIRECTION'
    sign = 1 if direction == 'BUY' else -1
    if regime != ('TREND_UP' if sign == 1 else 'TREND_DOWN'):
        return False, 'ADA_TREND_NOT_ALIGNED'
    if x[34]*sign <= 0 or x[35]*sign <= 0:
        return False, 'BTC_NOT_ALIGNED'
    if x[6] < RULE['volume_ratio_1h_min']:
        return False, 'INSUFFICIENT_VOLUME'
    return True, 'FROZEN_RULE_PASSED'


def metrics(items):
    chosen = [r for r in items if r['accepted']]
    counts = {s:sum(r['status']==s for r in chosen) for s in ('HIT','FAIL','NEUTRAL','PENDING')}
    n = counts['HIT']+counts['FAIL']
    p = counts['HIT']/n if n else None
    # Descriptive Wilson interval; overlapping horizons are NOT independent evidence.
    ci = None
    if n:
        z=1.96; den=1+z*z/n; mid=(p+z*z/(2*n))/den
        half=z*math.sqrt(p*(1-p)/n+z*z/(4*n*n))/den
        ci=[mid-half,mid+half]
    return dict(decisions=len(items),signals=len(chosen),resolved=n,**counts,
                accuracy=p,descriptive_wilson_interval=ci,
                coverage=len(chosen)/len(items) if items else 0)


def historical(archive, output):
    rows=[]
    with zipfile.ZipFile(archive) as z:
        outcomes={r['time']:r for r in map(json.loads,io.TextIOWrapper(z.open('replay-output/outcomes.jsonl')))}
        for r in map(json.loads,io.TextIOWrapper(z.open('replay-output/decisions.jsonl'))):
            x=r['variables']; regime=classify(x)
            for engine in ('kotlin','alternative'):
                e=r[engine]
                rows.append(dict(time=r['time'],engine=engine,regime=regime,
                    direction=e['direction'],accepted=e['accepted'],
                    status=outcomes[r['time']][engine]['status']))
            accept,reason=selection(x,r['kotlin']['direction'],r['kotlin']['accepted'])
            rows.append(dict(time=r['time'],engine='candidate',regime=regime,
                direction=r['kotlin']['direction'],accepted=accept,reason=reason,
                status=outcomes[r['time']]['kotlin']['status']))
    report=dict(status='EXPLORATORY_ALREADY_INSPECTED_HISTORY',rule=RULE,rule_sha256=RULE_HASH,
        source_archive_sha256=hashlib.sha256(Path(archive).read_bytes()).hexdigest(),
        overall={e:metrics([r for r in rows if r['engine']==e]) for e in ('kotlin','alternative','candidate')},
        by_regime={g:{e:metrics([r for r in rows if r['engine']==e and r['regime']==g])
            for e in ('kotlin','alternative','candidate')} for g in sorted({r['regime'] for r in rows})},
        by_year={str(y):{e:metrics([r for r in rows if r['engine']==e and dt.datetime.fromtimestamp(r['time']/1000,dt.timezone.utc).year==y])
            for e in ('kotlin','candidate')} for y in (2023,2024,2025,2026)},
        limitations=['Rule defined after inspecting the overall historical examination. Exploratory only.',
            'No threshold tuning or probability claim. Alternative selected zero signals.',
            'Simple mean-based research regime, not replacement for production regime taxonomy.',
            'Overlapping 24h labels; Wilson interval descriptive only.',
            'No profitability or execution simulation; no production activation.',
            'Prospective cohort is delayed and sparse, distinct from this historical reconstruction.'])
    Path(output).write_text(json.dumps(report,indent=2)+'\n')
    return report


def live_variables(data, t):
    x=[]
    for name,step in (('ADA_1h',HOUR),('ADA_4h',4*HOUR),('ADA_1d',24*HOUR),('BTC_4h',4*HOUR)):
        with (data/(name+'.csv')).open() as f:
            rows=[r for r in csv.DictReader(f) if int(r['closeTime'])<=t][-200:]
        if len(rows)!=200 or int(rows[-1]['closeTime']) != ((t+1)//step)*step-1:
            return None
        if any(int(r['closeTime']) != int(r['openTime'])+step-1 or int(r['openTime'])%step for r in rows):
            return None
        if any(int(b['openTime'])-int(a['openTime'])!=step for a,b in zip(rows,rows[1:])):
            return None
        values=[[float(r[k]) for k in ('open','high','low','close','volume')] for r in rows]
        if any(not all(math.isfinite(v) for v in a) or min(a[:4])<=0 or a[4]<0 or a[1]<max(a[0],a[2],a[3]) or a[2]>min(a[0],a[1],a[3]) for a in values):
            return None
        c=[v[3] for v in values]; vols=[v[4] for v in values]
        returns=[math.log(b/a) for a,b in zip(c,c[1:])]
        x.extend([c[-1]/c[-1-k]-1 for k in (1,4,12,24)])
        x.extend([statistics.pstdev(returns[-24:]),statistics.mean(v[1]-v[2] for v in values[-14:])/c[-1],
            vols[-1]/max(statistics.mean(vols[-20:]),1e-12),c[-1]/statistics.mean(c[-50:])-1,c[-1]/statistics.mean(c)-1])
    return x


def capture(root, data, version):
    from prospective_ledger import append, resolve
    root=Path(root); data=Path(data); now=int(time.time()*1000)
    events=[json.loads(s) for s in (root/'events.jsonl').read_text().splitlines()]
    shadow=root/'regime_shadow'; shadow.mkdir(parents=True,exist_ok=True)
    path=shadow/'events.jsonl'
    history=[json.loads(s) for s in path.read_text().splitlines()] if path.exists() else []
    decisions=[e for e in events if e['type']=='DECISION']
    if decisions:
        base=max(decisions,key=lambda e:e['recorded_at'])
        identifier=base['id']+'-'+RULE_HASH
        # Never backfill historical observations or record after the outcome window begins.
        if (0<=now-base['recorded_at']<=10*60000 and now<base['first_open'] and
            not any(e['type']=='DECISION' and e['id']==identifier for e in history)):
            manifest=json.loads((data/'manifest.json').read_text())
            for filename,digest in manifest['sha256'].items():
                if hashlib.sha256((data/filename).read_bytes()).hexdigest()!=digest:
                    raise ValueError('Market input hash mismatch')
            x=live_variables(data,base['candle_close'])
            accepted,reason=selection(x,base['direction'],base['accepted'])
            record={**base,'id':identifier,'base_id':base['id'],'recorded_at':now,
                'version':version,'rule':RULE,'rule_sha256':RULE_HASH,'regime':classify(x),
                'variables':x,'accepted':base['accepted'],'base_accepted':base['accepted'],
                'hypothetical_filter_accepted':accepted,'mode':'OBSERVATION_ONLY_REJECTED_FILTER',
                'rejection':reason,'engine':'shadow_regime_filter','probability':None}
            append(path,record); history.append(record)
    with (data/'ADA_5m.csv').open() as f:
        fine=list(csv.DictReader(f))
    resolved={r['id'] for r in history if r['type']=='OUTCOME'}
    for r in history:
        if r['type']!='DECISION' or not r['base_accepted'] or r['id'] in resolved:
            continue
        result=resolve(r,fine,now)
        if result is not None:
            append(path,dict(type='OUTCOME',id=r['id'],evaluated_at=now,**result))
            resolved.add(r['id'])
    final=[json.loads(s) for s in path.read_text().splitlines()] if path.exists() else []
    labels={r['id']:r for r in final if r['type']=='OUTCOME'}
    observed=[dict(regime=r['regime'],accepted=r['accepted'],hypothetical=r['hypothetical_filter_accepted'],
        status=labels.get(r['id'],{}).get('status','PENDING')) for r in final if r['type']=='DECISION']
    summary={g:{'baseline':metrics([r for r in observed if r['regime']==g]),
        'rejected_filter_shadow':metrics([{**r,'accepted':r['hypothetical']} for r in observed if r['regime']==g])}
        for g in sorted({r['regime'] for r in observed})}
    (shadow/'summary.json').write_text(json.dumps(dict(mode='OBSERVATION_ONLY',rule_sha256=RULE_HASH,
        by_regime=summary,probability_claim=None,production_changed=False),indent=2)+'\n')
    print('Shadow decisions:',sum(r['type']=='DECISION' for r in history),'resolved:',len(resolved))


if __name__=='__main__':
    p=argparse.ArgumentParser(); sub=p.add_subparsers(dest='mode',required=True)
    h=sub.add_parser('historical');h.add_argument('--archive',required=True);h.add_argument('--output',required=True)
    c=sub.add_parser('capture');c.add_argument('--root',required=True);c.add_argument('--data',required=True);c.add_argument('--version',required=True)
    a=p.parse_args()
    if a.mode=='historical':
        r=historical(a.archive,a.output);print(json.dumps(r['overall'],indent=2))
    else: capture(a.root,a.data,a.version)
