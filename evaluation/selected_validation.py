"""Append future paired predictions without retroactive fills or model replacement."""
import argparse
import base64
import csv
import datetime as dt
import hashlib
import json
import pickle
from pathlib import Path
import time
import numpy as np
import expanded_replay as replay
from expanded_history import INTERVALS
from prospective_ledger import resolve

PROTOCOL = Path(__file__).with_name('selected_validation_protocol.json')


def load_events(root):
    return [json.loads(line) for p in sorted(root.glob('events-*.jsonl'))
            for line in p.read_text().splitlines() if line]


def append(root,event):
    month=dt.datetime.fromtimestamp(event['recorded_at']/1000,dt.timezone.utc).strftime('%Y-%m')
    with (root/('events-'+month+'.jsonl')).open('a') as f:
        f.write(json.dumps(event,sort_keys=True)+'\n')


def checked_model(folder,protocol_hash,lock=None):
    manifest=json.loads((folder/'model_manifest.json').read_text())
    if manifest['protocol_sha256'] != protocol_hash:
        raise ValueError('Model belongs to a different registered protocol')
    if manifest['status'] != 'FROZEN_FOR_NEW_PROSPECTIVE_VALIDATION':
        return None,manifest
    if manifest['calibration_label_end']>manifest['cutoff'] or manifest['cutoff']>=manifest['examination_start']:
        raise ValueError('Model preparation crosses prospective boundary')
    for counts in manifest['counts'].values():
        if counts['latest_train_label']>=manifest['train_end'] or counts['latest_calibration_label']>=manifest['calibration_label_end']:
            raise ValueError('Labels leak across preparation boundaries')
    blob=(folder/'frozen_model.pkl').read_bytes()
    digest=hashlib.sha256(blob).hexdigest()
    if digest!=manifest['model_sha256'] or (lock and digest!=lock):
        raise ValueError('Frozen model changed or hash mismatch')
    # Only repository-owned workflow artifacts are supplied; never accept user pickle files.
    return pickle.loads(blob),manifest


def is_fresh(close,recorded,now,tf):
    return 0<=recorded-close<=INTERVALS[tf]+15*60000 and 0<=now-recorded<=15*60000


def capture(root,data,decision_file,model_root,version,engine_sha,now=None):
    injected_clock=now is not None
    now=now if injected_clock else int(time.time()*1000)
    protocol=json.loads(PROTOCOL.read_text()); phash=hashlib.sha256(PROTOCOL.read_bytes()).hexdigest()
    start=int(dt.datetime.fromisoformat(protocol['start_utc']).timestamp()*1000)
    root.mkdir(parents=True,exist_ok=True)
    history=load_events(root)
    if any(e['protocol_sha256']!=phash for e in history): raise ValueError('Registered protocol changed')
    if any(e.get('engine_sha256',engine_sha)!=engine_sha for e in history): raise ValueError('Original engine changed during validation')
    locked={e['symbol']+'-'+e['timeframe']:e['alternative']['model_sha256'] for e in history
            if e['type']=='DECISION' and e['alternative'].get('model_sha256')}
    known={e['id'] for e in history if e['type']=='DECISION'}
    manifest=json.loads((data/'manifest.json').read_text())
    arrays={}
    def array(name):
        if name not in arrays:
            path=data/(name+'.csv')
            if hashlib.sha256(path.read_bytes()).hexdigest()!=manifest['inputs'][path.name]['sha256']:
                raise ValueError('Current input hash mismatch')
            arrays[name]=np.loadtxt(path,delimiter=',',skiprows=1,ndmin=2)
        return arrays[name]
    for line in decision_file.read_text().splitlines():
        symbol,tf,close,recorded,accepted,side,entry,target,first,end,price_time,snapshot=line.split('\t')
        close,recorded,first,end,price_time=map(int,(close,recorded,first,end,price_time))
        if [symbol,tf] not in protocol['selections']: raise ValueError('Unregistered combination')
        if recorded<start: continue
        identifier=symbol+'-'+tf+'-'+str(close)
        if identifier in known: continue
        if not is_fresh(close,recorded,now,tf): raise ValueError('Stale or pre-registration decision; never backfill')
        if price_time>recorded or recorded-price_time>60*60000: raise ValueError('Stale/future entry price')
        if first!=(recorded//300000+1)*300000 or end!=first+replay.horizon(tf)-1:
            raise ValueError('Outcome starts before registration or wrong horizon')
        base64.b64decode(snapshot,validate=True)
        alt=dict(accepted=False,direction='WAIT',probability=None,model_sha256=None,reason='MODEL_NOT_AVAILABLE')
        model_dir=model_root/(symbol+'-'+tf)
        if (model_dir/'model_manifest.json').exists():
            models,model_manifest=checked_model(model_dir,phash,locked.get(symbol+'-'+tf))
            alt['reason']=model_manifest['status']
            if models:
                names=[(symbol+'USDT_'+k,INTERVALS[k]) for k in replay.contexts(tf)]
                if symbol!='BTC': names.append(('BTCUSDT_4h',INTERVALS['4h']))
                x=replay.Features({n:array(n) for n,step in names},dict(names)).at(close)
                if x is None: raise ValueError('Invalid latest alternative context')
                scores=[float(cal.predict_proba(model.decision_function([x]).reshape(-1,1))[0,1]) for model,cal in models]
                index=int(np.argmax(scores)); prob=scores[index]
                alt.update(accepted=prob>=protocol['threshold_alternative'],direction=('BUY','SELL')[index],
                    probability=prob,model_sha256=model_manifest['model_sha256'],reason='' if prob>=.65 else 'BELOW_FROZEN_THRESHOLD')
        # The durable journal time is later than Kotlin computation. Outcomes must
        # start AFTER this append, never after an earlier buffered computation.
        computed_at=recorded
        recorded=now if injected_clock else int(time.time()*1000)
        first=(recorded//300000+1)*300000
        if first-recorded<1000: first+=300000
        end=first+replay.horizon(tf)-1
        event=dict(type='DECISION',id=identifier,symbol=symbol,timeframe=tf,candle_close=close,
            recorded_at=recorded,computed_at=computed_at,version=version,engine_sha256=engine_sha,protocol_sha256=phash,
            entry=float(entry),target_pct=float(target),first_open=first,horizon_end=end,
            entry_price_time=price_time,context_snapshot_base64=snapshot,
            original=dict(accepted=accepted=='true',direction=side),alternative=alt,
            data_hashes={k:v['sha256'] for k,v in manifest['inputs'].items() if k.startswith(symbol+'USDT_') or k=='BTCUSDT_4h.csv'})
        append(root,event);history.append(event);known.add(identifier)
    resolved={(e['id'],e['engine']) for e in history if e['type']=='OUTCOME'}
    fine_cache={}
    for event in history[:]:
        if event['type']!='DECISION' or now<=event['horizon_end']:continue
        if not any(event[e]['accepted'] and (event['id'],e) not in resolved for e in ('original','alternative')):continue
        symbol=event['symbol']
        if symbol not in fine_cache:
            path=data/(symbol+'USDT_5m.csv')
            with path.open() as f: fine_cache[symbol]=list(csv.DictReader(f))
        candles=fine_cache[symbol]
        for engine in ('original','alternative'):
            if not event[engine]['accepted'] or (event['id'],engine) in resolved:continue
            prediction=dict(event,direction=event[engine]['direction'])
            result=resolve(prediction,candles,now)
            if result is not None:
                outcome=dict(type='OUTCOME',id=event['id'],engine=engine,recorded_at=now,
                    protocol_sha256=phash,**result)
                append(root,outcome);history.append(outcome);resolved.add((event['id'],engine))
    summaries=[]
    for symbol,tf in protocol['selections']:
        decisions=[e for e in history if e['type']=='DECISION' and e['symbol']==symbol and e['timeframe']==tf]
        ids={e['id'] for e in decisions}
        for engine in ('original','alternative'):
            outcomes=[e for e in history if e['type']=='OUTCOME' and e['id'] in ids and e['engine']==engine]
            counts={s:sum(e['status']==s for e in outcomes) for s in ('HIT','FAIL','NEUTRAL')}
            signals=sum(e[engine]['accepted'] for e in decisions); n=counts['HIT']+counts['FAIL']
            summaries.append(dict(symbol=symbol,timeframe=tf,engine=engine,decisions=len(decisions),signals=signals,
                pending=signals-len(outcomes),accuracy=counts['HIT']/n if n else None,**counts))
    status='AWAITING_REGISTERED_START' if now<start else 'COLLECTING_NO_CONFIRMED_IMPROVEMENT'
    (root/'summary.json').write_text(json.dumps(dict(status=status,protocol_sha256=phash,
        updated_at=now,combinations=summaries,winner=None),indent=2)+'\n')
    print(status,'decisions',sum(e['type']=='DECISION' for e in history),'outcomes',len(resolved))


if __name__=='__main__':
    p=argparse.ArgumentParser()
    for key in ('root','data','decisions','models'):p.add_argument('--'+key,type=Path,required=True)
    p.add_argument('--version',required=True);p.add_argument('--engine-sha256',required=True)
    a=p.parse_args();capture(a.root,a.data,a.decisions,a.models,a.version,a.engine_sha256)
