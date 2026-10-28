"""Audit existing chronological XRP replay, never train or change a predictor."""
import argparse
import csv
import datetime as dt
import hashlib
import json
from collections import Counter
from pathlib import Path
from frozen_engine_digest import engine_digest

STATUSES = ('HIT', 'APPROXIMATE', 'LATE', 'FAIL', 'PENDING')
DAY = 86400000


def summarize(rows):
    c = Counter(r['status'] for r in rows)
    if set(c) - set(STATUSES):
        raise ValueError('Unknown outcome')
    n = len(rows) - c['PENDING']
    return dict(signals=len(rows), resolved=n, **{s: c[s] for s in STATUSES},
                exact_rate=c['HIT']/n if n else None,
                exact_or_approximate_rate=(c['HIT']+c['APPROXIMATE'])/n if n else None)


def first_episodes(rows):
    """Transitive 120h overlap within direction; selection ignores outcomes."""
    chosen = []
    for side in ('BUY', 'SELL'):
        end = -1
        for r in sorted((r for r in rows if r['direction'] == side), key=lambda r: r['time']):
            if r['time'] > end:
                chosen.append(r)
            end = max(end, r['time'] + 5*DAY)
    return sorted(chosen, key=lambda r: r['time'])


def audit(source, output):
    source, output = Path(source), Path(output)
    work = source/'magnitude-output'
    labels = {}
    with (work/'labels.tsv').open() as f:
        for v in csv.reader(f, delimiter='\t'):
            key = int(v[0]), v[1]
            if key in labels or v[2] not in STATUSES:
                raise ValueError('Duplicate/invalid label')
            labels[key] = v[2]
    with (source/'replay-kotlin/kotlin_accepted.csv').open() as f:
        accepted = {int(r['signalCloseTime']) for r in csv.DictReader(f)}
    rows, last, decisions, feature_times = [], -1, Counter(), set()
    with (work/'features.jsonl').open() as f:
        for line in f:
            r = json.loads(line)
            t = r['time']
            if t <= last:
                raise ValueError('Unordered/duplicate decisions')
            last = t
            feature_times.add(t)
            year = str(dt.datetime.fromtimestamp(t/1000, dt.timezone.utc).year)
            decisions[year] += 1
            if r['original_accepted'] != (t in accepted):
                raise ValueError('Acceptance mismatch')
            if not r['original_accepted'] or r['original_direction'] == 'WAIT':
                continue
            side = 'SELL' if 'SELL' in r['original_direction'] else 'BUY'
            rows.append(dict(time=t, direction=side, year=year, phase=r['phase'],
                             entry=r['entry'], target=r['target'], status=labels[t, side]))
    manifest = json.loads((work/'prelabel-manifest.json').read_text())
    root = Path(__file__).resolve().parents[1]
    digest = engine_digest(root)
    if digest != 'f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7':
        raise ValueError('Predictor changed; cannot reuse old decisions')
    criterion = root/'app/src/main/java/com/domingales/criptoanalisis/multi/domain/ForecastCriterion.kt'
    if hashlib.sha256(criterion.read_bytes()).hexdigest() != manifest['evaluator_sha256']:
        raise ValueError('Criterion changed; cannot reuse old labels')
    if sum(decisions.values()) != manifest['rows']:
        raise ValueError('Feature row mismatch')
    inputs = {}
    starts, ends = [], []
    for name, expected in manifest['inputs'].items():
        m = json.loads((source/'replay-data'/f'{name}_manifest.json').read_text())
        if m['csv_sha256'] != expected:
            raise ValueError('Input provenance mismatch')
        starts.append(m['start']); ends.append(m['end'])
        inputs[name] = m
    report = dict(scope='ORIGINAL_XRP_30m_ONLY', production_changed=False,
        predictor_sha256=digest, criterion_sha256=manifest['evaluator_sha256'],
        trained_or_recalibrated=False, reused_chronological_source_run=40580602201,
        criterion='MAGNITUDE_TIME_V1', overall=summarize(rows),
        by_year={str(y): dict(evaluable_decisions=decisions[str(y)],
                    **summarize([r for r in rows if r['year'] == str(y)])) for y in range(2020, 2027)},
        by_direction={s: summarize([r for r in rows if r['direction'] == s]) for s in ('BUY','SELL')},
        by_phase={p: summarize([r for r in rows if r['phase'] == p]) for p in sorted({r['phase'] for r in rows})},
        first_transitive_episode_by_direction=summarize(first_episodes(rows)),
        first_evaluable_time=min(feature_times), last_evaluable_time=max(feature_times),
        archived_data_start=min(starts), archived_data_end=max(ends),
        accepted_without_features=len(accepted-feature_times),
        inputs=inputs,
        evidence_sha256={str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in [work/'features.jsonl', work/'labels.tsv', work/'prelabel-manifest.json',
                      source/'replay-kotlin/kotlin_accepted.csv']},
        limits=['Existing as-of chronological decisions reused, not a fresh independent experiment.',
                '2020 and early 2021 are indicator/context warm-up, not zero-accuracy predictions.',
                'No frozen 2023-trained candidate applied to earlier dates.',
                'Original engine threshold81, closed candles, neutral absent historical news.',
                'Magnitude-time HIT is not first-touch operational HIT or profitability.',
                'Overlapping windows and episode outcomes remain dependent.',
                'Historical data ends 2026-10-02, not today; no fabricated later candles.',
                'Only XRP30m; does not certify all assets or the Android live cohort.'])
    output.mkdir(parents=True, exist_ok=True)
    (output/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    with (output/'accepted-outcomes.jsonl').open('w') as f:
        for r in rows:
            f.write(json.dumps(r)+'\n')
    print(json.dumps(report, indent=2))
    return report


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--source', required=True)
    p.add_argument('--output', required=True)
    a = p.parse_args()
    audit(a.source, a.output)
