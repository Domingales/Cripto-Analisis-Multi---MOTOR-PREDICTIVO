"""Durable collection for the 15 prespecified pairs; research branch only."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import selected_validation as validation

ROOT=Path('evaluation/selected-prospective')
BRANCH='research/expanded-replay-150'


def git(*args):
    return subprocess.check_output(['git',*args],text=True).strip()


def push_fast_forward():
    head=git('rev-parse','HEAD'); parent=git('rev-parse','HEAD^')
    for attempt in range(3):
        try:
            git('push','origin','HEAD:'+BRANCH); return
        except subprocess.CalledProcessError:
            git('fetch','origin',BRANCH); remote=git('rev-parse','FETCH_HEAD')
            if remote==head: return
            if remote!=parent or attempt==2: raise
            time.sleep(attempt+1)


def persist(message):
    git('add',str(ROOT))
    if subprocess.run(['git','diff','--cached','--quiet']).returncode:
        git('commit','-m',message); push_fast_forward()
    return git('rev-parse','HEAD')


def digest(event):
    return hashlib.sha256(json.dumps(event,sort_keys=True).encode()).hexdigest()


def verify(events,receipts):
    for event in events:
        if event['type']!='DECISION': continue
        receipt=receipts.get(event['id'])
        if not receipt or receipt['event_sha256']!=digest(event):
            raise ValueError('Decision lacks a verified remote durability receipt')
        if not event['recorded_at']<=receipt['confirmed_at']<event['first_open']:
            raise ValueError('Remote registration missed the outcome boundary')


def capture():
    protocol=json.loads(validation.PROTOCOL.read_text())
    phash=hashlib.sha256(validation.PROTOCOL.read_bytes()).hexdigest()
    lock=json.loads((ROOT/'freeze-lock.json').read_text())
    if lock['protocol_sha256']!=phash: raise ValueError('Frozen protocol changed')
    engine=subprocess.check_output('sha256sum app/src/main/java/com/domingales/criptoanalisis/multi/domain/*.kt | sha256sum',shell=True,text=True).split()[0]
    if lock['engine_sha256']!=engine: raise ValueError('Frozen Kotlin engine changed')
    for symbol,tf in protocol['selections']:
        folder=ROOT/'models'/(symbol+'-'+tf)
        model,manifest=validation.checked_model(folder,phash,lock['models'].get(symbol+'-'+tf))
        if model is None and manifest['status']=='FROZEN_FOR_NEW_PROSPECTIVE_VALIDATION':
            raise ValueError('Evaluable frozen model cannot be loaded')
    receipts_path=ROOT/'durability-receipts.json'
    receipts=json.loads(receipts_path.read_text()) if receipts_path.exists() else {}
    before=validation.load_events(ROOT); verify(before,receipts)
    before_ids={e['id'] for e in before if e['type']=='DECISION'}
    pending=[]
    validation.capture(ROOT,Path('selected-data'),Path('selected-output/decisions.tsv'),ROOT/'models',
                       git('rev-parse','HEAD'),engine,durable_sink=pending.append)
    after=validation.load_events(ROOT)
    after_ids={e['id'] for e in after if e['type']=='DECISION'}
    if not before_ids.issubset(after_ids): raise ValueError('Previously durable decisions lost')
    if pending:
        sha=persist('Registrar decisiones de 15 combinaciones antes de sus ventanas futuras')
        confirmed=int(time.time()*1000)
        for event in pending:
            receipts[event['id']]=dict(event_sha256=digest(event),commit=sha,confirmed_at=confirmed,
                                       run_id=os.environ.get('GITHUB_RUN_ID'))
        receipts_path.write_text(json.dumps(receipts,indent=2)+'\n')
        verify(pending,receipts)
        persist('Confirmar escritura remota de las 15 combinaciones')
    verify(after,receipts)
    runs_path=ROOT/'collection-runs.json'
    runs=json.loads(runs_path.read_text()) if runs_path.exists() else []
    runs.append(dict(run_id=os.environ.get('GITHUB_RUN_ID','local'),recovered_decisions=len(before_ids),
                     new_decisions=len(after_ids-before_ids),total_decisions=len(after_ids),
                     protocol_sha256=phash,completed_at=int(time.time()*1000)))
    runs_path.write_text(json.dumps(runs,indent=2)+'\n')
    persist('Conservar continuidad de las 15 combinaciones')
    print(json.dumps(runs[-1],indent=2))


if __name__=='__main__': capture()
