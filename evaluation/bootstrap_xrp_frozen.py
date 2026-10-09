"""Persist the already checked preparation artifact once; never retrain."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import selected_validation as validation
import xrp_episode_validation as xrp
from xrp_periodic_capture import ROOT,persist

RUN=37775349894
ARTIFACT=11549284298
PREPARATION_SHA='6cd24603a15b49234a6de7c6c34efc680d25e0d7'


def bootstrap():
    if (ROOT/'freeze-lock.json').exists(): return
    if ROOT.exists() and any(ROOT.iterdir()):
        raise ValueError('Partial bootstrap exists: refuse to replace a frozen model')
    repo=os.environ['GITHUB_REPOSITORY']
    info=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/actions/runs/{RUN}']))
    if info['conclusion']!='success' or info['head_sha']!=PREPARATION_SHA:
        raise ValueError('Unverified preparation run')
    artifact=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/actions/artifacts/{ARTIFACT}']))
    if artifact['expired'] or artifact['workflow_run']['id']!=RUN:
        raise ValueError('Frozen preparation artifact unavailable')
    temp=Path('verified-xrp-preparation')
    subprocess.run(['gh','run','download',str(RUN),'--repo',repo,'--name',artifact['name'],'--dir',str(temp)],check=True)
    candidates=list(temp.rglob('xrp-model/XRP-30m/model_manifest.json'))
    if len(candidates)!=1: raise ValueError('Ambiguous frozen model location')
    folder=candidates[0].parent
    phash=hashlib.sha256(xrp.PROTOCOL.read_bytes()).hexdigest()
    model,manifest=validation.checked_model(folder,phash)
    if model is None: raise ValueError('Frozen model not evaluable')
    engine=subprocess.check_output('sha256sum app/src/main/java/com/domingales/criptoanalisis/multi/domain/*.kt | sha256sum',shell=True,text=True).split()[0]
    for path in Path('app/src/main/java/com/domingales/criptoanalisis/multi/domain').glob('*.kt'):
        old=subprocess.check_output(['git','show',PREPARATION_SHA+':'+str(path)])
        if path.read_bytes()!=old: raise ValueError('Production engine differs from verified preparation')
    shutil.copytree(folder,ROOT/'models/XRP-30m')
    lock=dict(model_sha256=manifest['model_sha256'],protocol_sha256=phash,engine_sha256=engine,
              preparation_run=RUN,preparation_artifact=ARTIFACT,preparation_sha=PREPARATION_SHA)
    (ROOT/'freeze-lock.json').write_text(json.dumps(lock,indent=2)+'\n')
    persist('Conservar modelo XRP30m congelado y verificado sin reentrenamiento')


if __name__=='__main__': bootstrap()
