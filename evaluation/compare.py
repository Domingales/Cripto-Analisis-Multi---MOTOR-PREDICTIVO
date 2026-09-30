#!/usr/bin/env python3
"""Same-candle comparison. Retrospective evidence, never independent validation."""
import argparse
import csv
import hashlib
import json
import math
import pathlib


def wilson(hits, fails):
    n = hits + fails
    if not n:
        return None
    p, z = hits / n, 1.959963984540054
    den = 1 + z*z/n
    centre = (p + z*z/(2*n)) / den
    half = z*math.sqrt(p*(1-p)/n + z*z/(4*n*n)) / den
    return [100*(centre-half), 100*(centre+half)]


def metrics(rows):
    h, f, neutral = (sum(r['outcome'] == s for r in rows) for s in ('HIT', 'FAIL', 'NEUTRAL'))
    return dict(signals=len(rows), hits=h, fails=f, neutral=neutral,
                pending=sum(r['outcome'] == 'PENDING' for r in rows),
                accuracy=100*h/(h+f) if h+f else None, wilson95=wilson(h, f))


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--base', required=True)
    p.add_argument('--candidate', required=True)
    p.add_argument('--output', required=True)
    args = p.parse_args()
    base, candidate = pathlib.Path(args.base), pathlib.Path(args.candidate)
    hashes = {}
    for original in sorted((base / 'data').glob('*.csv')):
        other = candidate / 'data' / original.name
        digest = hashlib.sha256(original.read_bytes()).hexdigest()
        if not other.exists() or digest != hashlib.sha256(other.read_bytes()).hexdigest():
            raise ValueError(f'Different market data: {original.name}')
        hashes[original.name] = digest
    def read(root):
        with (root / 'output/ada_1h_casos.csv').open() as f:
            return list(csv.DictReader(f))
    b, c = read(base), read(candidate)
    key = lambda r: (r['symbol'], r['timeframe'], r['signalCloseTime'], r['direction'])
    bm, cm = {key(r): r for r in b}, {key(r): r for r in c}
    if len(bm) != len(b) or len(cm) != len(c):
        raise ValueError('Duplicate signals')
    common = bm.keys() & cm.keys()
    report = dict(base_commit='501819f916634486d2aeecfcdbe29a6d97d6d135',
                  sha256=hashes, comparison='retrospective_same_candles',
                  base=metrics(b), candidate=metrics(c), common=len(common),
                  added=len(cm.keys()-bm.keys()), removed=len(bm.keys()-cm.keys()),
                  changed=sum(bm[k] != cm[k] for k in common),
                  directions={side: dict(base=metrics([r for r in b if side in r['direction']]),
                                         candidate=metrics([r for r in c if side in r['direction']]))
                              for side in ('BUY', 'SELL')},
                  independent_validation=False,
                  limitations='Wilson assumes independent cases; overlapping horizons weaken that assumption. '
                  'Same-candle comparison is descriptive; use future cohorts to validate predictive changes.')
    pathlib.Path(args.output).write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
