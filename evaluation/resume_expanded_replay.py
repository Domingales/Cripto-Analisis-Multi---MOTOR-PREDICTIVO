"""Resume data-only replay repairs while preserving the exact tested engine release."""
import argparse
import hashlib
import json
import os
import subprocess
from pathlib import Path
from expanded_history import assets, INTERVALS

ROOT = Path(__file__).resolve().parents[1]
SOURCE_RUN = 37607394740
SOURCE_HEAD = 'c8bfcc00d3d07d2d6527bb1688aa06653c376987'
ENGINE_VERSION = '68b0335ca0c1a8aed44b1f20393c84bb25c42543'
TRAIN_END = 1680307200000
TEST_START = 1696204800000
CUT = 1790953200000
ALLOWED = {
    'evaluation/reconcile_official_history.py',
    'evaluation/test_reconcile_official_history.py',
    'evaluation/resume_expanded_replay.py',
    'evaluation/test_resume_expanded_replay.py',
    '.github/workflows/expanded-replay.yml',
}

def gh(repository, route):
    return json.loads(subprocess.check_output(['gh', 'api', 'repos/' + repository + '/' + route], text=True))

def verify_engine_source(repository):
    commit = gh(repository, 'git/commits/' + ENGINE_VERSION)
    tree = gh(repository, 'git/trees/' + commit['tree']['sha'] + '?recursive=1')
    if tree.get('truncated'):
        raise ValueError('Source tree truncated; cannot prove engine identity')
    old = {r['path']:dict(sha=r['sha'],mode=r['mode']) for r in tree['tree'] if r['type']=='blob'}
    current = {}
    for line in subprocess.check_output(['git','ls-tree','-r','HEAD'],text=True).splitlines():
        entry,path = line.split('\t',1)
        mode,kind,digest = entry.split()
        if kind=='blob':
            current[path]=dict(sha=digest,mode=mode)
    changed = sorted(p for p in old.keys() | current.keys() if old.get(p)!=current.get(p))
    forbidden = [p for p in changed if p not in ALLOWED and not p.startswith('docs/engine-reviews/')]
    if forbidden:
        raise ValueError('Predictive release changed; unsafe to reuse prior reports: '+str(forbidden))
    return dict(engine_release=ENGINE_VERSION, source_head=SOURCE_HEAD,
                current_execution_commit=os.environ.get('GITHUB_SHA'),
                permitted_data_pipeline_changes=changed,
                unchanged_release_blobs={p:d for p,d in old.items() if p not in ALLOWED and not p.startswith('docs/engine-reviews/')})

def validate_prior(summary):
    expected = {(s,tf) for s in assets(ROOT) for tf in INTERVALS}
    reports = {}
    hashes = {}
    for r in summary['reports']:
        key = (r['symbol'],r['timeframe'])
        if key not in expected or key in reports:
            raise ValueError('Unexpected or duplicate historical combination')
        f = r['freeze']
        h = (336 if key[1]=='1d' else 72 if key[1]=='4h' else 24)*3600000
        if (f['version']!=ENGINE_VERSION or f['threshold']!=.65 or f['horizon_ms']!=h or
            f['train_end']!=TRAIN_END or f['examination_start']!=TEST_START or
            f['calibration_label_end']!=TEST_START-h or f['cutoff']!=CUT):
            raise ValueError('Historical engine release or temporal protocol mismatch')
        for side,c in f['counts'].items():
            if (c['latest_train_label'] is not None and c['latest_train_label']>=TRAIN_END or
                c['latest_calibration_label'] is not None and c['latest_calibration_label']>=TEST_START-h):
                raise ValueError('Historical future label cannot be reused')
        for name,digest in f['inputs'].items():
            if len(digest)!=64 or (name in hashes and hashes[name]!=digest):
                raise ValueError('Mixed historical input versions')
            hashes[name]=digest
        for engine in ('kotlin','alternative'):
            m=r[engine]
            if (m['signals']!=sum(m[x] for x in ('HIT','FAIL','NEUTRAL','PENDING')) or
                m['resolved']!=m['HIT']+m['FAIL'] or m['signals']>m['decisions']):
                raise ValueError('Historical metric arithmetic mismatch')
            accuracy=m['HIT']/m['resolved'] if m['resolved'] else None
            if m['accuracy']!=accuracy:
                raise ValueError('Historical accuracy mismatch')
        if f['status']=='FROZEN_BEFORE_EXAMINATION':
            if not f['model_sha256'] or set(f['counts'])!={'BUY','SELL'} or any(
                c['train']<100 or c['calibration']<100 for c in f['counts'].values()):
                raise ValueError('Historical frozen preparation mismatch')
        elif (f['status']!='NOT_EVALUABLE_INSUFFICIENT_PRE2023_TRAINING_OR_CALIBRATION' or
              f['model_sha256'] is not None or r['alternative']['signals']!=0):
            raise ValueError('Historical non-evaluable status mismatch')
        reports[key]=r
    missing=expected-set(reports)
    if summary['completed']!=len(reports) or summary['expected']!=len(expected) or set(map(tuple,summary['missing']))!=missing:
        raise ValueError('Historical coverage metadata mismatch')
    # Recompute every interval of a changed asset, preserving shared-input identity.
    rerun_symbols={s for s,tf in missing}
    reused={k:r for k,r in reports.items() if k[0] not in rerun_symbols}
    rerun=sorted(expected-set(reused))
    return reused,rerun

def prepare(repository,summary_path,output,materialize=False):
    proof=verify_engine_source(repository)
    run=gh(repository,'actions/runs/'+str(SOURCE_RUN))
    if run['status']!='completed' or run['head_sha']!=SOURCE_HEAD or run['run_attempt']!=1:
        raise ValueError('Source experiment must be complete and match the tested release')
    summary=json.loads(summary_path.read_text())
    reused,rerun=validate_prior(summary)
    proof.update(source_run=SOURCE_RUN, source_run_conclusion=run['conclusion'],
                 source_summary_sha256=hashlib.sha256(summary_path.read_bytes()).hexdigest(),
                 reused_combinations=[list(k) for k in sorted(reused)],
                 rerun_combinations=[list(k) for k in rerun],
                 original_report_model_hashes={s+'_'+tf:r['freeze']['model_sha256'] for (s,tf),r in reused.items()})
    output.mkdir(parents=True,exist_ok=True)
    (output/'recovery-provenance.json').write_text(json.dumps(proof,indent=2)+'\n')
    if materialize:
        for (s,tf),r in reused.items():
            target=output/('reused-'+s+'-'+tf)
            target.mkdir(parents=True,exist_ok=True)
            (target/'report.json').write_text(json.dumps(r,indent=2)+'\n')
    return dict(include=[dict(symbol=s,timeframe=tf) for s,tf in rerun])

def stamp(output):
    manifest=json.loads((output/'model_manifest.json').read_text())
    if manifest['version']!=ENGINE_VERSION:
        raise ValueError('Recovery model release mismatch')
    proof=json.loads(Path('prior-validation/recovery-provenance.json').read_text())
    proof=dict(engine_release=ENGINE_VERSION, execution_commit=os.environ.get('GITHUB_SHA'),
        source_run=SOURCE_RUN, model_sha256=manifest['model_sha256'],inputs=manifest['inputs'],
        permitted_data_pipeline_changes=proof['permitted_data_pipeline_changes'])
    (output/'recovery_provenance.json').write_text(json.dumps(proof,indent=2)+'\n')

if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--repository');p.add_argument('--source-summary',type=Path)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--materialize',action='store_true');p.add_argument('--stamp',action='store_true')
    a=p.parse_args()
    if a.stamp:
        stamp(a.output)
    else:
        print(json.dumps(prepare(a.repository,a.source_summary,a.output,a.materialize),separators=(',',':')))
