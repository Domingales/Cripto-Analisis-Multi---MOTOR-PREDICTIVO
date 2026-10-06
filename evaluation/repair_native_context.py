"""Fill incomplete derived buckets only from checksum-verified native Binance bars.

Never modify 5m outcomes or synthesize OHLCV. Complete overlapping prices must
agree with the native archive; volume source variants are audited without replacement; maintenance gaps stay explicit in the 5m series.
"""
import argparse
import csv
import datetime as dt
import hashlib
import json
import math
from pathlib import Path
import download_long_history as history
from expanded_history import INTERVALS, save


def merge_native(derived, native, step, volume_discrepancies=None):
    result = dict(derived)
    added = []
    for t, row in sorted(native.items()):
        if t % step or int(row[6]) != t + step - 1:
            raise ValueError('Nonstandard native candle')
        if t in result:
            old = result[t]
            if int(old[6]) != int(row[6]) or any(
                not math.isclose(float(a), float(b), rel_tol=1e-9, abs_tol=1e-8)
                for a, b in zip(old[1:5], row[1:5])
            ):
                raise ValueError('Native/derived OHLC conflict at ' + str(t) + '; derived=' + str(old) + '; native=' + str(row))
            if not math.isclose(float(old[5]),float(row[5]),rel_tol=1e-9,abs_tol=1e-8):
                if volume_discrepancies is not None:
                    volume_discrepancies.append(dict(open_time=t,derived_volume=float(old[5]),native_volume=float(row[5]),
                        policy='KEEP_COMPLETE_5M_DERIVED_BUCKET_UNCHANGED'))
        else:
            result[t] = row
            added.append(t)
    return result, added


def repair(root, symbol, interval):
    name = symbol + 'USDT_' + interval
    path = root / (name + '.csv')
    manifest_path = root / (name + '_manifest.json')
    original = json.loads(manifest_path.read_text())
    if hashlib.sha256(path.read_bytes()).hexdigest() != original['csv_sha256']:
        raise ValueError('Original input hash mismatch')
    with path.open() as f:
        rows = {int(r[0]): [int(r[0]), *r[1:6], int(r[6])]
                for r in list(csv.reader(f))[1:]}
    if not rows:
        raise ValueError('No listing history')
    step = INTERVALS[interval]
    history.STEPS[interval] = step
    # No attempt to invent bars before listing or after the last real bucket.
    gaps = history.missing_ranges(rows, interval, min(rows), max(rows) + step)
    months = set()
    for start, end in gaps:
        for t in range(start, end, step):
            months.add(dt.datetime.fromtimestamp(t / 1000, dt.timezone.utc).strftime('%Y-%m'))
    evidence = dict(symbol=symbol+'USDT', interval=interval, sources=[], errors=[], anomalies=[], volume_discrepancies=[])
    native = {}
    for month in sorted(months):
        if not history.archive(symbol+'USDT', interval, 'monthly', month, native, evidence):
            raise RuntimeError('Official native context repair failed: '+name+' '+month)
    rows, added = merge_native(rows, native, step, evidence['volume_discrepancies'])
    evidence['added_open_times'] = added
    evidence_path = root / (name + '_native_repair_sources.json')
    evidence_path.write_text(json.dumps(evidence, indent=2)+'\n')
    updated = save(symbol+'USDT', interval, rows, root, dict(
        type='COMPLETE_5M_BUCKETS_WITH_VERIFIED_NATIVE_GAP_REPAIR',
        original_derived_sha256=original['csv_sha256'],
        repair_sources_sha256=hashlib.sha256(evidence_path.read_bytes()).hexdigest()))
    updated['native_repair_count'] = len(added)
    updated['native_volume_discrepancy_count'] = len(evidence['volume_discrepancies'])
    manifest_path.write_text(json.dumps(updated, indent=2)+'\n')
    print(name, 'native repairs', len(added), 'remaining missing', updated['missing_candles'], 'volume variants retained', updated['native_volume_discrepancy_count'], flush=True)
    return updated


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--data', required=True)
    p.add_argument('--symbol', required=True)
    p.add_argument('--interval', choices=INTERVALS)
    a = p.parse_args()
    for tf in [a.interval] if a.interval else INTERVALS:
        repair(Path(a.data), a.symbol, tf)
