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
import numpy as np
import download_long_history as history
from expanded_history import INTERVALS, save

PARENT_CACHE = {}


def carry_price_convention(fine,t,step,derived,native):
    """Prove a source convention, never accept arbitrary price discrepancies."""
    i=int(np.searchsorted(fine[:,0],t)); w=fine[i:i+step//300000]
    if (len(w)!=step//300000 or not len(w) or w[0,0]!=t or
        w[-1,6]!=t+step-1 or np.any(np.diff(w[:,0])!=300000)):
        return None
    traded=w[w[:,5]>0]
    if not len(traded) or len(traded)==len(w):return None
    def prices(a):return [a[0,1],max(a[:,2]),min(a[:,3]),a[-1,4]]
    same=lambda a,b:all(math.isclose(float(x),float(y),rel_tol=1e-9,abs_tol=1e-8) for x,y in zip(a,b))
    if not same(derived[1:5],prices(w)) or not same(native[1:5],prices(traded)):
        return None
    if not math.isclose(float(derived[5]),float(sum(w[:,5])),rel_tol=1e-9,abs_tol=1e-8):
        return None
    return dict(open_time=t,derived_prices=list(map(float,derived[1:5])),
        native_prices=list(map(float,native[1:5])),
        zero_volume_open_times=list(map(int,w[w[:,5]==0,0])),
        reason='KNOWN_CARRY_PRICE_CANDLES_VS_ACTUAL_TRADE_OHLC',
        policy='KEEP_COMPLETE_5M_DERIVED_BUCKET_UNCHANGED')


def merge_native(derived, native, step, volume_discrepancies=None,
                 price_checker=None, price_conventions=None):
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
                convention=price_checker(t,old,row) if price_checker else None
                if convention is None:
                    raise ValueError('Native/derived OHLC conflict at ' + str(t) + '; derived=' + str(old) + '; native=' + str(row))
                if price_conventions is not None:price_conventions.append(convention)
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
    evidence = dict(symbol=symbol+'USDT', interval=interval, sources=[], errors=[], anomalies=[],
        volume_discrepancies=[],price_conventions=[])
    native = {}
    for month in sorted(months):
        if not history.archive(symbol+'USDT', interval, 'monthly', month, native, evidence):
            raise RuntimeError('Official native context repair failed: '+name+' '+month)
    def price_checker(t,old,new):
        reconciliation=root/(symbol+'USDT_reconciliation.json')
        if reconciliation.exists():
            audit=json.loads(reconciliation.read_text())
            m=json.loads(manifest_path.read_text())
            if (audit['status']!='CORROBORATED_RECONCILIATION_COMPLETE' or
                hashlib.sha256(reconciliation.read_bytes()).hexdigest()!=m['origin'].get('reconciliation_sha256')):
                raise ValueError('Reconciliation evidence hash/status mismatch')
            for proof in audit['conflicts']:
                if (proof['timeframe']==interval and proof['open_time']==t and
                    proof.get('policy') in ('KEEP_CORROBORATED_DERIVED',
                        'USE_CORROBORATED_DAILY_5M_REBUILD_ALL_INTERVALS',
                        'USE_THIRD_VARIANT_CORROBORATED_DAILY_5M_AND_NATIVE_REBUILD_ALL_INTERVALS') and
                    all(math.isclose(float(a),float(b),rel_tol=1e-9,abs_tol=1e-8)
                        for a,b in zip(old[1:6],proof['daily_native'][1:6])) and
                    all(math.isclose(float(a),float(b),rel_tol=1e-9,abs_tol=1e-8)
                        for a,b in zip(new[1:6],proof['monthly_native'][1:6]))):
                    return dict(proof,reason='CORROBORATED_DAILY_ARCHIVE_VARIANT',
                                reconciliation_sha256=hashlib.sha256(reconciliation.read_bytes()).hexdigest())
        parent=root/(symbol+'USDT_5m.csv')
        if not parent.exists():return None
        key=str(parent.resolve())
        if key not in PARENT_CACHE:
            m=json.loads((root/(symbol+'USDT_5m_manifest.json')).read_text())
            if hashlib.sha256(parent.read_bytes()).hexdigest()!=m['csv_sha256']:
                raise ValueError('Parent 5m input hash mismatch')
            PARENT_CACHE[key]=np.loadtxt(parent,delimiter=',',skiprows=1,ndmin=2)
        return carry_price_convention(PARENT_CACHE[key],t,step,old,new)
    rows, added = merge_native(rows,native,step,evidence['volume_discrepancies'],
        price_checker,evidence['price_conventions'])
    evidence['added_open_times'] = added
    evidence_path = root / (name + '_native_repair_sources.json')
    evidence_path.write_text(json.dumps(evidence, indent=2)+'\n')
    updated = save(symbol+'USDT', interval, rows, root, dict(
        type='COMPLETE_5M_BUCKETS_WITH_VERIFIED_NATIVE_GAP_REPAIR',
        original_derived_sha256=original['csv_sha256'],
        repair_sources_sha256=hashlib.sha256(evidence_path.read_bytes()).hexdigest()))
    updated['native_repair_count'] = len(added)
    updated['native_volume_discrepancy_count'] = len(evidence['volume_discrepancies'])
    updated['verified_carry_price_convention_count'] = sum(
        p.get('reason')=='KNOWN_CARRY_PRICE_CANDLES_VS_ACTUAL_TRADE_OHLC' for p in evidence['price_conventions'])
    updated['verified_source_reconciliation_count'] = sum(
        p.get('reason')=='CORROBORATED_DAILY_ARCHIVE_VARIANT' for p in evidence['price_conventions'])
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
