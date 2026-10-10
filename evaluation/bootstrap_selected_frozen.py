"""Persist the already prepared 15-pair artifacts once; never retrain."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import selected_validation as validation
from selected_periodic_capture import ROOT,persist


def bootstrap():
    if (ROOT/'freeze-lock.json').exists(): return
    if ROOT.exists() and any(ROOT.iterdir()): raise ValueError('Partial bootstrap exists')
    protocol=json.loads(validation.PROTOCOL.read_text())
    phash=hashlib.sha256(validation.PROTOCOL.read_bytes()).hexdigest()
    repo=os.environ['GITHUB_REPOSITORY']; temp=Path('verified-selected-models')
    models={}; statuses={}
    for symbol,tf in protocol['selections']:
        name='prospective-frozen-'+symbol+'-'+tf
        response=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/actions/artifacts?name={name}&per_page=20']))
        artifact=next((a for a in response['artifacts'] if not a['expired']),None)
        if not artifact: raise ValueError('Missing non-expired registered artifact: '+name)
        target=temp/(symbol+'-'+tf)
        subprocess.run(['gh','run','download',str(artifact['workflow_run']['id']),'--repo',repo,'--name',name,'--dir',str(target)],check=True)
        manifests=list(target.rglob('model_manifest.json'))
        if len(manifests)!=1: raise ValueError('Ambiguous model artifact: '+name)
        folder=manifests[0].parent
        model,manifest=validation.checked_model(folder,phash)
        statuses[symbol+'-'+tf]=manifest['status']
        if model is not None: models[symbol+'-'+tf]=manifest['model_sha256']
        shutil.copytree(folder,ROOT/'models'/(symbol+'-'+tf))
    unavailable=sorted(k for k,v in statuses.items() if v!='FROZEN_FOR_NEW_PROSPECTIVE_VALIDATION')
    if unavailable!=['ARB-1d']:
        raise ValueError('Unexpected model availability: '+json.dumps(unavailable))
    engine=subprocess.check_output('sha256sum app/src/main/java/com/domingales/criptoanalisis/multi/domain/*.kt | sha256sum',shell=True,text=True).split()[0]
    (ROOT/'freeze-lock.json').write_text(json.dumps(dict(protocol_sha256=phash,engine_sha256=engine,
        models=models,statuses=statuses,source='REGISTERED_EXPANDED_REPLAY_ARTIFACTS_NO_RETRAINING'),indent=2)+'\n')
    persist('Conservar modelos congelados de las 15 combinaciones sin reentrenar')


if __name__=='__main__': bootstrap()
