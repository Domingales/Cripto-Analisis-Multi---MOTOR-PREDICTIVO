"""Corroborate archive conflicts before native context repair. Research only.

The daily 5m aggregate AND daily native bar must agree on all OHLCV fields.
When both monthly variants are wrong, the independently aggregated daily
consensus is retained explicitly as a third variant; it is never hidden.
Keep original inputs, every changed 5m row, archive hashes and rejected evidence.
Never fill 5m gaps or choose a source solely because it is daily/monthly.
"""
import argparse
import csv
import datetime as dt
import hashlib
import json
import math
import shutil
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import download_long_history as history
from expanded_history import INTERVALS, aggregate, save
from repair_native_context import carry_price_convention
import numpy as np


def same(a, b):
    return int(a[0]) == int(b[0]) and int(a[6]) == int(b[6]) and all(
        math.isclose(float(x), float(y), rel_tol=1e-9, abs_tol=1e-8)
        for x, y in zip(a[1:6], b[1:6]))


def choose_variant(parent, derived, monthly, daily_fine, daily_native, step):
    t = int(derived[0])
    times = list(range(t, t + step, 300000))
    if any(k not in parent or k not in daily_fine for k in times):
        raise ValueError('UNRESOLVED_CONFLICT_MISSING_5M_PARENT_OR_DAILY')
    original = aggregate({k: parent[k] for k in times}, step)[t]
    candidate = aggregate({k: daily_fine[k] for k in times}, step)[t]
    if not same(original, derived):
        raise ValueError('UNRESOLVED_CONFLICT_DERIVED_DOES_NOT_REPRODUCE_PARENT')
    if daily_native is None or not same(candidate, daily_native):
        raise ValueError('UNRESOLVED_CONFLICT_DAILY_5M_NATIVE_DISAGREE')
    if same(candidate, derived):
        return 'KEEP_CORROBORATED_DERIVED', []
    return ('USE_CORROBORATED_DAILY_5M_REBUILD_ALL_INTERVALS' if same(candidate, monthly)
            else 'USE_THIRD_VARIANT_CORROBORATED_DAILY_5M_AND_NATIVE_REBUILD_ALL_INTERVALS'), [
        dict(open_time=k, before=parent[k], after=daily_fine[k])
        for k in times if not same(parent[k], daily_fine[k])]


def read_verified(root, symbol, tf):
    p = root / f'{symbol}_{tf}.csv'
    m = json.loads((root / f'{symbol}_{tf}_manifest.json').read_text())
    if hashlib.sha256(p.read_bytes()).hexdigest() != m['csv_sha256']:
        raise ValueError('Input hash mismatch: ' + str(p))
    with p.open() as f:
        reader = csv.reader(f); next(reader)
        rows = {int(r[0]): [int(r[0]), *r[1:6], int(r[6])] for r in reader}
    if len(rows) != m['count']:
        raise ValueError('Input count mismatch')
    return rows, m


def reconcile(root, symbol):
    symbol += 'USDT'
    audit_path = root / f'{symbol}_reconciliation.json'
    if audit_path.exists():
        raise ValueError('Reconciliation already attempted; restore original snapshot first')
    parent, manifest = read_verified(root, symbol, '5m')
    original_parent = dict(parent)
    fine = np.array([r for _, r in sorted(parent.items())], dtype=float)
    audit = dict(symbol=symbol, status='IN_PROGRESS', input_sha256=manifest['csv_sha256'],
                 sources=[], errors=[], anomalies=[], conflicts=[], changed_5m_rows=[])
    cache = {}

    def archive(tf, frequency, date):
        key = (tf, frequency, date)
        if key not in cache:
            rows = {}; history.STEPS[tf] = 300000 if tf == '5m' else INTERVALS[tf]
            if not history.archive(symbol, tf, frequency, date, rows, audit):
                raise RuntimeError('Official corroboration unavailable: ' + str(key))
            cache[key] = rows
        return cache[key]

    snapshot = root / 'original-inputs' / symbol
    snapshot.mkdir(parents=True, exist_ok=False)
    for tf in ['5m', *INTERVALS]:
        for suffix in ['.csv', '_manifest.json']:
            p = root / f'{symbol}_{tf}{suffix}'
            shutil.copy2(p, snapshot / p.name)
    try:
        # Each interval uses the original derived inputs for independent evidence.
        for tf, step in INTERVALS.items():
            history.STEPS[tf] = step
            derived, _ = read_verified(root, symbol, tf)
            gaps = history.missing_ranges(derived, tf, min(derived), max(derived) + step)
            months = sorted({dt.datetime.fromtimestamp(t / 1000, dt.timezone.utc).strftime('%Y-%m')
                             for a, b in gaps for t in range(a, b, step)})
            with ThreadPoolExecutor(max_workers=4) as pool:
                list(pool.map(lambda month: archive(tf, 'monthly', month), months))
            for month in months:
                for t, native in archive(tf, 'monthly', month).items():
                    old = derived.get(t)
                    if old is None or all(math.isclose(float(x), float(y), rel_tol=1e-9, abs_tol=1e-8)
                                          for x, y in zip(old[1:5], native[1:5])):
                        continue
                    if carry_price_convention(fine, t, step, old, native) is not None:
                        continue  # Existing rule proves and audits this separately.
                    date = dt.datetime.fromtimestamp(t / 1000, dt.timezone.utc).strftime('%Y-%m-%d')
                    evidence = dict(timeframe=tf, open_time=t, derived=old, monthly_native=native)
                    audit['conflicts'].append(evidence)
                    daily_fine = archive('5m', 'daily', date)
                    daily_native = archive(tf, 'daily', date).get(t)
                    evidence['daily_native'] = daily_native
                    policy, changes = choose_variant(original_parent, old, native, daily_fine, daily_native, step)
                    evidence['policy'] = policy
                    # Conflicting proposals across intervals must not overwrite one another.
                    for change in changes:
                        k = change['open_time']
                        if not same(parent[k], original_parent[k]) and not same(parent[k], change['after']):
                            raise ValueError('Conflicting corroborated 5m proposals')
                        parent[k] = change['after']
        audit['changed_5m_rows'] = [dict(open_time=t, before=original_parent[t], after=parent[t])
                                   for t in parent if not same(original_parent[t], parent[t])]
        audit['status'] = 'CORROBORATED_RECONCILIATION_COMPLETE'
        audit_path.write_text(json.dumps(audit, indent=2) + '\n')
        evidence_hash = hashlib.sha256(audit_path.read_bytes()).hexdigest()
        new = save(symbol, '5m', parent, root, dict(type='CORROBORATED_OFFICIAL_VARIANTS',
                   original_csv_sha256=manifest['csv_sha256'], reconciliation_sha256=evidence_hash))
        for tf, step in INTERVALS.items():
            save(symbol, tf, aggregate(parent, step), root, dict(type='EXACT_COMPLETE_5M_AGGREGATION',
                 parent_csv_sha256=new['csv_sha256'], reconciliation_sha256=evidence_hash))
        print(symbol, 'corroborated conflicts', len(audit['conflicts']),
              'changed 5m rows', len(audit['changed_5m_rows']), flush=True)
        return audit
    except Exception as e:
        audit['status'] = 'BLOCKED_UNRESOLVED_OFFICIAL_CONFLICT'
        audit['failure'] = str(e)
        audit_path.write_text(json.dumps(audit, indent=2) + '\n')
        raise


if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--data', type=Path, required=True)
    p.add_argument('--symbol', required=True); a = p.parse_args()
    reconcile(a.data, a.symbol)
