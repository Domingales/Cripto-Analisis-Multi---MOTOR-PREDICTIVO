"""Prospective contracts and orchestration; evaluation uses Android Kotlin itself."""
import argparse
import base64
import csv
import hashlib
import json
import math
from pathlib import Path
import re
import time

DAY = 86400000
EVALUATOR = Path('app/src/main/java/com/domingales/criptoanalisis/multi/domain/ForecastCriterion.kt')


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def new_contract(snapshot, recorded_at):
    text = base64.b64decode(snapshot, validate=True).decode()
    def field(name):
        found = re.search(r'\b' + name + r'=([-+0-9.eE]+)', text)
        return float(found.group(1)) if found else None
    low, high = field('forecastLowPct'), field('forecastHighPct')
    if low is None or high is None:
        return None  # Legacy/synthetic snapshots cannot fabricate a new forecast.
    if not all(math.isfinite(v) and v > 0 for v in (low, high)) or high < low:
        raise ValueError('Invalid forecast range')
    return dict(profile='MAGNITUDE_TIME_V1', issued_at=recorded_at,
                target_pct=(low + high) / 2, horizon_ms=DAY, grace_ms=2*DAY,
                expiry_ms=5*DAY, minimum_fraction=.75,
                evaluator_sha256=hashlib.sha256(EVALUATOR.read_bytes()).hexdigest(),
                target_source='ORIGINAL_FORECAST_RANGE_MIDPOINT_COMMON_TO_BOTH_ENGINES',
                limits='Independent cloud cohort; latest closed-5m entry, not mobile execution')


def events(root):
    return [json.loads(line) for p in sorted(root.glob('events-*.jsonl'))
            for line in p.read_text().splitlines() if line]


def eligible(root):
    receipts = json.loads((root/'durability-receipts.json').read_text())
    evaluator = hashlib.sha256(EVALUATOR.read_bytes()).hexdigest()
    rows = []
    for e in events(root):
        if e['type'] != 'DECISION' or not e.get('magnitude_time'):
            continue
        c = e['magnitude_time']; r = receipts.get(e['id'])
        if c['profile'] != 'MAGNITUDE_TIME_V1' or c['evaluator_sha256'] != evaluator:
            raise ValueError('Registered criterion changed')
        if not r or r['event_sha256'] != digest(e) or not e['recorded_at'] <= r['confirmed_at'] < e['first_open']:
            raise ValueError('Missing timely remote registration')
        if c['issued_at'] != e['recorded_at']:
            raise ValueError('Criterion was not registered with the forecast')
        for engine in ('original', 'alternative'):
            if e[engine]['accepted']:
                rows.append((e, engine))
    return rows


def prepare(root, data, work):
    work.mkdir(parents=True, exist_ok=True)
    rows = eligible(root)
    finished_path = root/'magnitude-outcomes.jsonl'
    finished = [json.loads(s) for s in finished_path.read_text().splitlines()] if finished_path.exists() else []
    known = {(r['id'], r['engine']) for r in finished}
    now = int(time.time()*1000)
    tasks = []
    manifest = json.loads((data/'manifest.json').read_text())
    for e, engine in rows:
        if (e['id'], engine) in known:
            continue
        name = e['symbol']+'USDT_5m.csv'; path = data/name
        if hashlib.sha256(path.read_bytes()).hexdigest() != manifest['inputs'][name]['sha256']:
            raise ValueError('Market evidence hash mismatch')
        c = e['magnitude_time']
        tasks.append(dict(id=e['id'], engine=engine, symbol=e['symbol'], timeframe=e['timeframe'],
                          entry=e['entry'], direction=e[engine]['direction'], contract=c,
                          data_sha256=manifest['inputs'][name]['sha256']))
    with (work/'contracts.tsv').open('w', newline='') as f:
        writer = csv.writer(f, delimiter='\t', lineterminator='\n')
        for t in tasks:
            c=t['contract']
            writer.writerow([t['id'],t['engine'],t['symbol'],t['entry'],t['direction'],c['issued_at'],
                             c['target_pct'],c['horizon_ms'],c['grace_ms'],c['expiry_ms'],c['minimum_fraction'],now])
    (work/'evidence.json').write_text(json.dumps(dict(evaluated_at=now,tasks=tasks),sort_keys=True))


def finish(root, work):
    rows=eligible(root)  # Recheck registration before appending any result.
    metadata=json.loads((work/'evidence.json').read_text())
    tasks={(t['id'],t['engine']): t for t in metadata['tasks']}
    output=list(csv.reader((work/'results.tsv').open(), delimiter='\t'))
    keys=[(r[0],r[1]) for r in output]
    if len(keys)!=len(set(keys)) or set(keys)!=set(tasks):
        raise ValueError('Kotlin results do not match registered tasks')
    path=root/'magnitude-outcomes.jsonl'
    outcomes=[json.loads(s) for s in path.read_text().splitlines()] if path.exists() else []
    existing={(o['id'],o['engine']):o for o in outcomes}
    for r in output:
        if len(r)!=8: raise ValueError('Malformed Kotlin result')
        if r[2] not in ('PENDING','HIT','APPROXIMATE','LATE','FAIL'):
            raise ValueError('Unexpected Kotlin status')
        if r[2]=='PENDING': continue
        task=tasks[(r[0],r[1])]
        result=dict(id=r[0],engine=r[1],status=r[2],reason=r[3],first_target_at=r[4],
                    first_approximate_at=r[5],mfe=float(r[6]),mae=float(r[7]),
                    evaluated_at=metadata['evaluated_at'],contract_sha256=digest(task['contract']),
                    evaluator_sha256=task['contract']['evaluator_sha256'],data_sha256=task['data_sha256'])
        previous=existing.get((r[0],r[1]))
        if previous is not None:
            if previous!=result: raise ValueError('Final result cannot be replaced')
            continue
        outcomes.append(result)
        with path.open('a') as f: f.write(json.dumps(result,sort_keys=True)+'\n')
    by_key={(o['id'],o['engine']):o for o in outcomes}
    summary=[]
    for engine in ('original','alternative'):
        selected=[e for e,side in rows if side==engine]
        counts={s:sum(by_key.get((e['id'],engine),{}).get('status')==s for e in selected)
                for s in ('HIT','APPROXIMATE','LATE','FAIL')}
        resolved=sum(counts.values())
        # Fixed non-overlap windows; retain only first signal per symbol/timeframe/engine.
        ends={}; independent=[]
        for e in sorted(selected,key=lambda e:e['recorded_at']):
            key=(e['symbol'],e['timeframe'])
            if e['recorded_at'] <= ends.get(key,-1): continue
            independent.append(e['id']); ends[key]=e['recorded_at']+5*DAY
        summary.append(dict(engine=engine,signals=len(selected),pending=len(selected)-resolved,**counts,
                            exact_rate=counts['HIT']/resolved if resolved else None,
                            approximate_or_exact_rate=(counts['HIT']+counts['APPROXIMATE'])/resolved if resolved else None,
                            first_nonoverlap_ids=independent))
    report=dict(profile='MAGNITUDE_TIME_V1',updated_at=metadata['evaluated_at'],
                combinations=summary,status='COLLECTING_NO_CONFIRMED_IMPROVEMENT',
                registered_decisions=sum(e['type']=='DECISION' and bool(e.get('magnitude_time')) for e in events(root)),
                limits='New contracts only; different directions/selection frequencies. Shared target from original range. Rates are descriptive, not independent-trial evidence or profitability.',winner=None)
    (root/'magnitude-summary.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['prepare','finish'])
    p.add_argument('--root',type=Path,required=True);p.add_argument('--work',type=Path,required=True)
    p.add_argument('--data',type=Path)
    a=p.parse_args()
    if a.action=='prepare': prepare(a.root,a.data,a.work)
    else: finish(a.root,a.work)
