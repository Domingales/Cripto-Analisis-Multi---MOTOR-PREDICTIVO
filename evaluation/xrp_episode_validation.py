"""Separate XRP cohort and outcome-blind episode summaries; no production changes."""
import argparse
import hashlib
import json
from pathlib import Path
import selected_validation as validation

PROTOCOL = Path(__file__).with_name('xrp30m_validation_protocol.json')


def episode_summary(decisions, outcomes):
    """Merge overlapping windows transitively; select first without seeing outcomes."""
    rows = sorted(decisions, key=lambda x: (x['first_open'], x['id']))
    if len({x['id'] for x in rows}) != len(rows):
        raise ValueError('Duplicate decision ID')
    groups = []
    for row in rows:
        if row['horizon_end'] < row['first_open']:
            raise ValueError('Invalid outcome window')
        if not groups or row['first_open'] > groups[-1]['end']:
            groups.append(dict(first=row, end=row['horizon_end'], ids=[]))
        groups[-1]['end'] = max(groups[-1]['end'], row['horizon_end'])
        groups[-1]['ids'].append(row['id'])
    counts = {s: 0 for s in ('HIT', 'FAIL', 'NEUTRAL', 'PENDING')}
    episodes = []
    for group in groups:
        first = group['first']
        status = outcomes.get(first['id'], 'PENDING')
        if status not in counts:
            raise ValueError('Unknown outcome')
        counts[status] += 1
        episodes.append(dict(first_id=first['id'], first_open=first['first_open'],
                             horizon_end=group['end'], signals=len(group['ids']), status=status))
    n = counts['HIT'] + counts['FAIL']
    return dict(episodes=episodes, counts=counts,
                accuracy=counts['HIT']/n if n else None,
                interpretation='Nonoverlapping windows do not establish statistical independence')


def summarize(root):
    history = validation.load_events(root)
    result = {}
    for engine in ('original', 'alternative'):
        decisions = [x for x in history if x['type'] == 'DECISION' and x[engine]['accepted']]
        outcomes = {x['id']: x['status'] for x in history
                    if x['type'] == 'OUTCOME' and x['engine'] == engine}
        result[engine] = episode_summary(decisions, outcomes)
    return result


def capture(args):
    # Separate process and ledger: the existing 15-pair protocol stays byte-identical.
    validation.PROTOCOL = PROTOCOL
    protocol_hash = hashlib.sha256(PROTOCOL.read_bytes()).hexdigest()
    folder = args.models/'XRP-30m'
    models, _ = validation.checked_model(folder, protocol_hash)
    if models is None:
        raise ValueError('XRP frozen prospective model not evaluable; do not start this cohort')
    validation.capture(args.root, args.data, args.decisions, args.models,
                       args.version, args.engine_sha256)
    (args.root/'episodes.json').write_text(json.dumps(summarize(args.root), indent=2)+'\n')


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('mode', choices=('prepare', 'capture', 'summarize'))
    for key in ('root', 'data', 'decisions', 'models', 'kotlin', 'output'):
        p.add_argument('--'+key, type=Path)
    p.add_argument('--version'); p.add_argument('--engine-sha256')
    a = p.parse_args()
    required = {'prepare': ('data', 'kotlin', 'output', 'version'),
                'capture': ('root', 'data', 'decisions', 'models', 'version', 'engine_sha256'),
                'summarize': ('root',)}[a.mode]
    if any(getattr(a, k) is None for k in required):
        p.error('Missing arguments: '+', '.join(required))
    if a.mode == 'prepare':
        import prepare_selected_models as preparation
        preparation.PROTOCOL = PROTOCOL
        preparation.prepare(a.data, a.kotlin, a.output, 'XRP', '30m', a.version)
    elif a.mode == 'capture':
        capture(a)
    else:
        print(json.dumps(summarize(a.root), indent=2))
