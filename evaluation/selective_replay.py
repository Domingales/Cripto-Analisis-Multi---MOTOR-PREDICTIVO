"""Outcome-blind selection on archived chronological decisions; research only."""
import argparse
import hashlib
import json
import math
from pathlib import Path

RULE = {'id': 'alternative-selective-090-v1', 'threshold': 0.90,
        'label': 'ARCHIVED_SYMMETRIC_FIRST_TOUCH_NOT_MAGNITUDE',
        'production_enabled': False}


def select(row):
    e = row['alternative']
    p = e.get('probability')
    if p is not None and (not math.isfinite(p) or not 0 <= p <= 1):
        raise ValueError('Invalid probability')
    return (row.get('variables') is not None and e['direction'] in ('BUY', 'SELL')
            and p is not None and p >= RULE['threshold'])


def first_episodes(rows):
    """Transitive interval union; first selected signal, never inspect labels."""
    first, end = [], None
    for r in rows:
        if end is None or r['time'] > end:
            first.append(r['time'])
            end = r['horizon_end']
        else:
            end = max(end, r['horizon_end'])
    return first


def metrics(rows):
    counts = {s: sum(r['status'] == s for r in rows)
              for s in ('HIT', 'FAIL', 'NEUTRAL', 'PENDING')}
    n = counts['HIT'] + counts['FAIL']
    mature = n + counts['NEUTRAL']
    return dict(signals=len(rows), **counts,
                resolved_accuracy=counts['HIT']/n if n else None,
                hit_fraction_all_mature=counts['HIT']/mature if mature else None)


def run(root, output):
    root, output = Path(root), Path(output)
    output.mkdir(parents=True, exist_ok=True)
    decisions_path = root/'decisions.jsonl'
    outcome_path = root/'outcomes.jsonl'
    manifest_path = root/'model_manifest.json'
    manifest = json.loads(manifest_path.read_text())
    if manifest['status'] != 'FROZEN_BEFORE_EXAMINATION':
        raise ValueError('Model not evaluable')
    if not manifest.get('model_sha256'):
        raise ValueError('Missing frozen model identity')
    rows = [json.loads(s) for s in decisions_path.read_text().splitlines()]
    if not rows:
        raise ValueError('Empty archive')
    times = [r['time'] for r in rows]
    if times != sorted(set(times)):
        raise ValueError('Duplicate or unordered decisions')
    if len({(r['symbol'], r['timeframe']) for r in rows}) != 1:
        raise ValueError('Mixed combinations')
    if not (manifest['train_end'] <= manifest['calibration_label_end']
            < manifest['examination_start']):
        raise ValueError('Invalid temporal boundaries')
    for r in rows:
        if r['time'] < manifest['examination_start'] or r['horizon_end'] <= r['time']:
            raise ValueError('Invalid examination time')
        if r['model_sha256'] != manifest['model_sha256']:
            raise ValueError('Mixed model identities')
    # Commit selection locally before opening any outcome file.
    selected = [r for r in rows if select(r)]
    journal = [{'time': r['time'], 'selected': select(r),
                'probability': r['alternative'].get('probability')}
               for r in rows]
    (output/'selection-before-outcomes.jsonl').write_text(
        ''.join(json.dumps(r, allow_nan=False)+'\n' for r in journal))
    first = first_episodes(selected)
    outcomes = [json.loads(s) for s in outcome_path.read_text().splitlines()]
    labels = {r['time']: r for r in outcomes}
    if len(labels) != len(outcomes) or set(labels) != set(times):
        raise ValueError('Unpaired/duplicate outcomes')
    def evaluate(chosen, engine):
        result = []
        for r in chosen:
            label = labels[r['time']][engine]
            if label['status'] not in ('HIT', 'FAIL', 'NEUTRAL', 'PENDING'):
                raise ValueError('Unknown outcome')
            result.append({'time': r['time'], **label})
        return metrics(result)
    report = dict(status='EXPLORATORY_ALREADY_INSPECTED_HISTORY', rule=RULE,
        combination=[rows[0]['symbol'], rows[0]['timeframe']], decisions=len(rows),
        model_sha256=manifest['model_sha256'],
        input_sha256={p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                      for p in (decisions_path, outcome_path, manifest_path)},
        evaluable_scores=sum(r['alternative'].get('probability') is not None for r in rows),
        candidate=evaluate(selected, 'alternative'),
        candidate_first_episode=evaluate([r for r in selected if r['time'] in set(first)], 'alternative'),
        episode_first_times=first, coverage=len(selected)/len(rows),
        baseline={e: evaluate([r for r in rows if r[e]['accepted']], e)
                  for e in ('kotlin', 'alternative')},
        same_windows_original=evaluate(selected, 'kotlin'),
        certified_probability=None, winner=None, production_changed=False,
        limitations=['Archived decisions, not a fresh raw-price replay or independent test.',
            'Training used earlier official history; scored examination begins in 2023, not 2020.',
            'No model retraining or change to frozen prospective collection.',
            '0.90 is a score selection threshold, NOT certified 90% accuracy.',
            'Transitive episode grouping does not establish statistical independence.',
            'Same-window original labels may have a different direction; not paired trades.',
            'First-touch outcomes are not MAGNITUDE_TIME_V1 or net profitability.'])
    (output/'report.json').write_text(json.dumps(report, indent=2, allow_nan=False)+'\n')
    print(json.dumps(report, indent=2))
    return report


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--root', required=True)
    p.add_argument('--output', required=True)
    a = p.parse_args()
    run(a.root, a.output)
