"""Collect only with the existing frozen model and remotely durable predictions."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import selected_validation as validation
import xrp_episode_validation as xrp

ROOT=Path('evaluation/prospective-xrp')
BRANCH='research/expanded-replay-150'


def git(*args):
    return subprocess.check_output(['git',*args],text=True).strip()


def persist(message):
    git('add',str(ROOT))
    if subprocess.run(['git','diff','--cached','--quiet']).returncode:
        git('commit','-m',message)
        # No force, rebase or overwrite: concurrent edits stop this collection.
        git('push','origin','HEAD:'+BRANCH)
    return git('rev-parse','HEAD')


def verify_receipts(events,receipts):
    for event in events:
        if event['type']!='DECISION': continue
        receipt=receipts.get(event['id'])
        digest=hashlib.sha256(json.dumps(event,sort_keys=True).encode()).hexdigest()
        if not receipt or receipt['event_sha256']!=digest:
            raise ValueError('Decision lacks a verified remote durability receipt')
        if not event['recorded_at']<=receipt['confirmed_at']<event['first_open']:
            raise ValueError('Remote registration missed the outcome boundary; cohort blocked')


def capture():
    validation.PROTOCOL=xrp.PROTOCOL
    phash=hashlib.sha256(xrp.PROTOCOL.read_bytes()).hexdigest()
    lock=json.loads((ROOT/'freeze-lock.json').read_text())
    model,manifest=validation.checked_model(ROOT/'models/XRP-30m',phash,lock['model_sha256'])
    if model is None: raise ValueError('Frozen model is not evaluable')
    if lock['protocol_sha256']!=phash: raise ValueError('Frozen protocol changed')
    # Use the identical engine digest construction as the verified preparation.
    engine=subprocess.check_output('sha256sum app/src/main/java/com/domingales/criptoanalisis/multi/domain/*.kt | sha256sum',shell=True,text=True).split()[0]
    if engine!=lock['engine_sha256']: raise ValueError('Frozen Kotlin engine changed')
    receipts_path=ROOT/'durability-receipts.json'
    receipts=json.loads(receipts_path.read_text()) if receipts_path.exists() else {}
    before=validation.load_events(ROOT)
    verify_receipts(before,receipts)
    before_ids={e['id'] for e in before if e['type']=='DECISION'}

    def remote_write(event):
        sha=persist('Registrar decisión XRP30m antes de su ventana futura')
        confirmed=int(time.time()*1000)
        receipts[event['id']]=dict(event_sha256=hashlib.sha256(json.dumps(event,sort_keys=True).encode()).hexdigest(),
                                   commit=sha,confirmed_at=confirmed,run_id=os.environ.get('GITHUB_RUN_ID'))
        receipts_path.write_text(json.dumps(receipts,indent=2)+'\n')
        verify_receipts([event],receipts)
        persist('Confirmar escritura remota XRP30m anterior a la evaluación')

    validation.capture(ROOT,Path('xrp-current-data'),Path('xrp-current-output/decisions.tsv'),ROOT/'models',
                       git('rev-parse','HEAD'),engine,durable_sink=remote_write)
    after=validation.load_events(ROOT)
    verify_receipts(after,receipts)
    after_ids={e['id'] for e in after if e['type']=='DECISION'}
    if not before_ids.issubset(after_ids): raise ValueError('Previously durable decisions lost')
    (ROOT/'episodes.json').write_text(json.dumps(xrp.summarize(ROOT),indent=2)+'\n')
    runs_path=ROOT/'collection-runs.json'
    runs=json.loads(runs_path.read_text()) if runs_path.exists() else []
    run_id=os.environ.get('GITHUB_RUN_ID','local')
    runs.append(dict(run_id=run_id,attempt=os.environ.get('GITHUB_RUN_ATTEMPT','1'),
                     recovered_decisions=len(before_ids),new_decisions=len(after_ids-before_ids),
                     total_decisions=len(after_ids),model_sha256=manifest['model_sha256'],
                     protocol_sha256=phash,completed_at=int(time.time()*1000)))
    runs_path.write_text(json.dumps(runs,indent=2)+'\n')
    persist('Conservar resultados y continuidad de la cohorte XRP30m')
    print(json.dumps(runs[-1],indent=2))


if __name__=='__main__': capture()
