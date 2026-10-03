"""Official Binance archives/API; hashes, validation and explicit unresolved gaps."""
import argparse
import calendar
import csv
import datetime as dt
import hashlib
import io
import json
import math
from pathlib import Path
import time
import urllib.error
import urllib.request
import zipfile

START = 1601596800000  # 2020-10-02 UTC
END = 1790953200000  # 2026-10-02 15:00 UTC, exclusive
STEPS = {'5m': 300000, '1h': 3600000, '4h': 14400000, '1d': 86400000}


def get(url):
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=45) as response:
                return response.read()
        except urllib.error.HTTPError as e:
            if e.code == 404 or attempt == 2:
                raise
        except (OSError, TimeoutError):
            if attempt == 2:
                raise
        time.sleep(2**attempt)


def timestamp(value):
    n = int(value)
    return n // 1000 if n > 10**14 else n


def normalize(row):
    return [timestamp(row[0]), *row[1:6], timestamp(row[6])]


def insert(candles, row, interval, start=START, end=END):
    r = normalize(row)
    step = STEPS[interval]
    if not start <= r[0] or r[6] >= end:
        return
    if r[0] % step or r[6] != r[0] + step - 1:
        raise ValueError('Invalid candle time/alignment')
    o, h, l, c, v = map(float, r[1:6])
    if (not all(math.isfinite(x) for x in (o, h, l, c, v)) or
            min(o, h, l, c) <= 0 or v < 0 or l > min(o, c) or h < max(o, c) or l > h):
        raise ValueError('Invalid OHLCV')
    # Decimal spellings can differ between API and archives; numeric values must agree.
    if r[0] in candles:
        old = candles[r[0]]
        if old[6] != r[6] or list(map(float, old[1:6])) != [o, h, l, c, v]:
            raise ValueError('Conflicting duplicate')
    else:
        candles[r[0]] = r


def missing_ranges(candles, interval, start=START, end=END):
    step = STEPS[interval]
    begin = None
    missing = []
    for t in range(start, end - end % step, step):
        if t not in candles and begin is None:
            begin = t
        if t in candles and begin is not None:
            missing.append([begin, t])
            begin = None
    if begin is not None:
        missing.append([begin, end - end % step])
    return missing


def archive(symbol, interval, frequency, date, candles, manifest):
    url = f'https://data.binance.vision/data/spot/{frequency}/klines/{symbol}/{interval}/{symbol}-{interval}-{date}.zip'
    try:
        blob = get(url)
        checksum = get(url + '.CHECKSUM').decode().split()[0]
        actual = hashlib.sha256(blob).hexdigest()
        if actual != checksum:
            raise ValueError('Checksum mismatch')
        staged = {}
        with zipfile.ZipFile(io.BytesIO(blob)) as package:
            for name in package.namelist():
                for row in csv.reader(io.TextIOWrapper(package.open(name))):
                    if row and row[0].isdigit():
                        insert(staged, row, interval)
        # Commit only after the whole source has passed validation.
        for row in staged.values():
            insert(candles, row, interval)
        manifest['sources'].append(dict(url=url, sha256=actual, verification='official_CHECKSUM'))
        print(f'{symbol} {interval} {date} checksum OK', flush=True)
        return True
    except ValueError:
        raise  # integrity failures must never be hidden by a fallback
    except Exception as e:
        manifest['errors'].append(dict(url=url, error=type(e).__name__ + ': ' + str(e)))
        return False


def api_range(symbol, interval, begin, end, candles, manifest):
    cursor = begin
    while cursor < end:
        url = f'https://data-api.binance.vision/api/v3/klines?symbol={symbol}&interval={interval}&startTime={cursor}&endTime={end-1}&limit=1000'
        blob = get(url)
        rows = json.loads(blob)
        if not isinstance(rows, list):
            raise ValueError('API did not return candles')
        manifest['sources'].append(dict(url=url, sha256=hashlib.sha256(blob).hexdigest(),
                                        verification='official_API_response_hash'))
        if not rows:
            break  # real exchange gaps remain explicit, never synthesize candles
        staged = {}
        for row in rows:
            insert(staged, row, interval, start=begin, end=end)
        for row in staged.values():
            insert(candles, row, interval)
        next_cursor = timestamp(rows[-1][0]) + STEPS[interval]
        if next_cursor <= cursor:
            raise ValueError('Pagination stalled')
        cursor = next_cursor


def run(symbol, interval, output):
    output.mkdir(parents=True, exist_ok=True)
    manifest = dict(symbol=symbol, interval=interval, start=START, end=END, sources=[], errors=[])
    candles = {}
    for year in range(2020, 2027):
        for month in range(1, 13):
            if not (2020, 10) <= (year, month) <= (2026, 9):
                continue
            if not archive(symbol, interval, 'monthly', f'{year}-{month:02d}', candles, manifest):
                # A missing monthly package (e.g. the most recent month) is not missing market history.
                for day in range(1, calendar.monthrange(year, month)[1] + 1):
                    archive(symbol, interval, 'daily', f'{year}-{month:02d}-{day:02d}', candles, manifest)
    archive(symbol, interval, 'daily', '2026-10-01', candles, manifest)
    # Try the official API for every unresolved range, including today's closed candles.
    manifest['missing_before_api'] = missing_ranges(candles, interval)
    for begin, end in manifest['missing_before_api']:
        try:
            api_range(symbol, interval, begin, end, candles, manifest)
        except ValueError:
            raise
        except Exception as e:
            manifest['errors'].append(dict(stage='gap_api', start=begin, end=end, error=str(e)))
    path = output / f'{symbol}_{interval}.csv'
    with path.open('w', newline='') as f:
        w = csv.writer(f)
        w.writerow(['openTime', 'open', 'high', 'low', 'close', 'volume', 'closeTime'])
        w.writerows(candles[t] for t in sorted(candles))
    missing = missing_ranges(candles, interval)
    manifest.update(count=len(candles), first=min(candles) if candles else None,
                    last=max(candles) if candles else None, missing_ranges=missing,
                    missing_candles=sum((b-a)//STEPS[interval] for a, b in missing),
                    csv_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                    status='COMPLETE' if not missing else 'INCOMPLETE_REQUIRES_REVIEW')
    (output / f'{symbol}_{interval}_manifest.json').write_text(json.dumps(manifest, indent=2))
    print(json.dumps({k: v for k, v in manifest.items() if k not in ('sources', 'errors')}, indent=2))
    if not candles:
        raise RuntimeError('No verified candles obtained')
    return manifest


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--symbol', choices=['ADAUSDT', 'BTCUSDT'], required=True)
    p.add_argument('--interval', choices=list(STEPS), required=True)
    p.add_argument('--output', required=True)
    a = p.parse_args()
    result = run(a.symbol, a.interval, Path(a.output))
    # Uploads still run with always(); a green job must not imply complete history.
    raise SystemExit(0 if result['status'] == 'COMPLETE' else 2)
