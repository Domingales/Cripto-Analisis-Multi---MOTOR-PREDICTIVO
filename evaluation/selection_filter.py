#!/usr/bin/env python3
"""Exploratory chronological selector; never changes production signals."""
import argparse
import csv
import json
import math
from pathlib import Path

MIN_CASES = 30
MIN_PROBABILITY = 0.65
HORIZON_MS = 24 * 60 * 60 * 1000


def bucket(row):
    return (row['symbol'], row['timeframe'], row['direction'], int(row['confidence']) // 10)


def wilson(hits, total):
    if not total:
        return None
    z = 1.959963984540054
    p = hits / total
    denominator = 1 + z*z/total
    centre = (p + z*z/(2*total)) / denominator
    radius = z * math.sqrt(p*(1-p)/total + z*z/(4*total*total)) / denominator
    return [centre-radius, centre+radius]


def evaluate(rows):
    ordered = sorted(rows, key=lambda r: int(r['signalCloseTime']))
    if len({(r['symbol'], r['timeframe'], r['signalCloseTime']) for r in ordered}) != len(ordered):
        raise ValueError('Duplicate signal')
    predictions = []
    for index, row in enumerate(ordered):
        timestamp = int(row['signalCloseTime'])
        # Entire horizon must have expired: conservative embargo also covers overlapping signals.
        history = [r for r in ordered[:index]
                   if bucket(r) == bucket(row)
                   and int(r['signalCloseTime']) + HORIZON_MS < timestamp
                   and r['outcome'] in ('HIT', 'FAIL')]
        n = len(history)
        hits = sum(r['outcome'] == 'HIT' for r in history)
        probability = (hits + 1) / (n + 2) if n >= MIN_CASES else None
        interval = wilson(hits, n)
        accepted = (probability is not None and probability >= MIN_PROBABILITY
                    and interval[0] > 0.5)
        predictions.append(dict(row, comparable_cases=n, estimated_probability=probability,
                                accepted=accepted, interval95=interval))
    def metrics(group):
        resolved = [r for r in group if r['outcome'] in ('HIT', 'FAIL')]
        hits = sum(r['outcome'] == 'HIT' for r in resolved)
        scored = [r for r in resolved if r.get('estimated_probability') is not None]
        return dict(signals=len(group), resolved=len(resolved), hits=hits,
                    fails=len(resolved)-hits, neutral=sum(r['outcome']=='NEUTRAL' for r in group),
                    accuracy=hits/len(resolved) if resolved else None,
                    interval95_descriptive=wilson(hits, len(resolved)),
                    brier=sum((r['estimated_probability']-(r['outcome']=='HIT'))**2
                              for r in scored)/len(scored) if scored else None,
                    probability_scored_cases=len(scored))
    selected = [r for r in predictions if r['accepted']]
    report = dict(status='EXPLORATORY_NOT_APPROVED_FOR_PRODUCTION',
                  rules=dict(min_cases=MIN_CASES, min_probability=MIN_PROBABILITY,
                             lower_interval_above=0.5, embargo_hours=24),
                  baseline=metrics(predictions), candidate=metrics(selected),
                  coverage=len(selected)/len(predictions) if predictions else 0,
                  by_direction={d: dict(baseline=metrics([r for r in predictions if r['direction']==d]),
                                       candidate=metrics([r for r in selected if r['direction']==d]))
                                for d in sorted({r['direction'] for r in predictions})},
                  limitations=['Previously inspected historical period; not independent validation.',
                               'Signals may be correlated; Wilson intervals are descriptive.',
                               'Confidence is a feature, not a calibrated probability.',
                               'Beta-smoothed frequencies require prospective calibration checks.',
                               'No costs, slippage, position sizing or executable return measured.',
                               'No automatic promotion of this selector.'])
    return report, predictions


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--cases', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    with open(args.cases, newline='') as source:
        report, predictions = evaluate(list(csv.DictReader(source)))
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    (output/'selection_report.json').write_text(json.dumps(report, indent=2)+'\n')
    (output/'selection_predictions.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in predictions))
    print(json.dumps(report, indent=2))
