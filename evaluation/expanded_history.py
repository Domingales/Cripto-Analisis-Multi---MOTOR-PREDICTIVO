"""Collect verified 5m history and derive only complete UTC OHLCV buckets."""
import argparse
import calendar
import csv
import datetime as dt
import hashlib
import json
import re
from pathlib import Path
import download_long_history as h

INTERVALS = {'15m': 900000, '30m': 1800000, '1h': 3600000, '4h': 14400000, '1d': 86400000}

def assets(root):
    source = root / 'app/src/main/java/com/domingales/criptoanalisis/multi/domain/Assets.kt'
    return re.findall(r'CryptoAsset\("([A-Z0-9]+)"', source.read_text())

def aggregate(candles, step):
    buckets = {}
    for t, row in sorted(candles.items()):
        buckets.setdefault(t // step * step, []).append(row)
    result = {}
    for start, rows in buckets.items():
        if [r[0] for r in rows] != list(range(start, start + step, 300000)):
            continue
        result[start] = [start, rows[0][1], max(float(r[2]) for r in rows),
                         min(float(r[3]) for r in rows), rows[-1][4],
                         sum(float(r[5]) for r in rows), start + step - 1]
    return result

def save(symbol, interval, rows, output, origin):
    step = 300000 if interval == '5m' else INTERVALS[interval]
    path = output / f'{symbol}_{interval}.csv'
    with path.open('w', newline='') as f:
        w = csv.writer(f)
        w.writerow(['openTime','open','high','low','close','volume','closeTime'])
        w.writerows(rows[t] for t in sorted(rows))
    # List every missing range including unavailable listing prefixes; never fabricate prices.
    h.STEPS[interval] = step
    missing = h.missing_ranges(rows, interval)
    manifest = dict(symbol=symbol, interval=interval, start=h.START, end=h.END,
        first=min(rows) if rows else None, last=max(rows) if rows else None,
        count=len(rows), missing_ranges=missing,
        missing_candles=sum((b-a)//step for a,b in missing),
        csv_sha256=hashlib.sha256(path.read_bytes()).hexdigest(), origin=origin,
        status='COMPLETE' if not missing else 'GAPS_OR_SHORTER_HISTORY_REQUIRES_REVIEW')
    (output / f'{symbol}_{interval}_manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    return manifest

def collect(symbol, output):
    output.mkdir(parents=True, exist_ok=True)
    manifest = dict(symbol=symbol, interval='5m', sources=[], errors=[], anomalies=[])
    candles = {}
    for year in range(2020, 2027):
        for month in range(1,13):
            if not (2020,10) <= (year,month) <= (2026,9):
                continue
            # Missing old packages stay explicit. API repairs only internal gaps,
            # avoiding thousands of requests before a coin's first listing.
            h.archive(symbol, '5m', 'monthly', f'{year}-{month:02d}', candles, manifest)
    h.archive(symbol, '5m', 'daily', '2026-10-01', candles, manifest)
    # API can recover an entire series when archives are unavailable or repair gaps.
    begin = min(candles) if candles else h.START
    for start,end in h.missing_ranges(candles, '5m', begin, h.END):
        try:
            h.api_range(symbol, '5m', start, end, candles, manifest)
        except ValueError:
            raise
        except Exception as e:
            manifest['errors'].append(dict(stage='API_GAP', start=start, end=end, error=str(e)))
    (output / f'{symbol}_sources.json').write_text(json.dumps(manifest, indent=2)+'\n')
    source_hash = hashlib.sha256((output / f'{symbol}_sources.json').read_bytes()).hexdigest()
    reports = [save(symbol, '5m', candles, output, dict(type='OFFICIAL_BINANCE', sources_sha256=source_hash))]
    parent_hash = reports[0]['csv_sha256']
    for interval,step in INTERVALS.items():
        reports.append(save(symbol, interval, aggregate(candles, step), output,
            dict(type='EXACT_COMPLETE_5M_AGGREGATION', parent_csv_sha256=parent_hash)))
    (output / 'coverage.json').write_text(json.dumps(reports, indent=2)+'\n')
    if not candles:
        raise RuntimeError('No verified candles; symbol is unavailable, not a completed experiment')
    print(json.dumps([dict(interval=r['interval'],count=r['count'],first=r['first'],status=r['status']) for r in reports]))

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--matrix', action='store_true')
    p.add_argument('--symbol')
    p.add_argument('--output', default='expanded-history')
    a = p.parse_args()
    symbols = assets(Path(__file__).resolve().parents[1])
    if a.matrix:
        print(json.dumps({'symbol': [s+'USDT' for s in symbols]}))
    elif a.symbol not in [s+'USDT' for s in symbols]:
        p.error('Symbol must belong to the current app catalog')
    else:
        collect(a.symbol, Path(a.output))
