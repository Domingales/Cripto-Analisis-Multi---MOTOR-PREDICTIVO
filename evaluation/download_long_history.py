"""Official Binance archives, checksum validation, explicit missing-data audit."""
import argparse
import csv
import datetime as dt
import hashlib
import io
import json
from pathlib import Path
import time
import urllib.error
import urllib.request
import zipfile

START = 1601596800000  # 2020-10-02 UTC
END = 1790953200000  # 2026-10-02 15:00 UTC
STEPS = {'5m':300000,'1h':3600000,'4h':14400000,'1d':86400000}


def get(url):
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=45) as response:
                return response.read()
        except urllib.error.HTTPError as e:
            if e.code == 404:
                raise
            if attempt == 2:
                raise
        except (OSError, TimeoutError):
            if attempt == 2:
                raise
        time.sleep(2**attempt)


def timestamp(value):
    n = int(value)
    return n//1000 if n > 10**14 else n


def normalize(row):
    return [timestamp(row[0]), *row[1:6], timestamp(row[6])]


def run(symbol, interval, output):
    output.mkdir(parents=True,exist_ok=True)
    manifest = dict(symbol=symbol,interval=interval,start=START,end=END,sources=[],errors=[])
    candles={}
    periods=[]
    for year in range(2020,2027):
        for month in range(1,13):
            if (year,month)<(2020,10) or (year,month)>(2026,9):
                continue
            periods.append(('monthly',f'{year}-{month:02d}'))
    periods.append(('daily','2026-10-01'))
    for frequency,date in periods:
        url=f'https://data.binance.vision/data/spot/{frequency}/klines/{symbol}/{interval}/{symbol}-{interval}-{date}.zip'
        try:
            blob=get(url); checksum=get(url+'.CHECKSUM').decode().split()[0]
            actual=hashlib.sha256(blob).hexdigest()
            if actual != checksum:
                raise ValueError('Checksum mismatch')
            with zipfile.ZipFile(io.BytesIO(blob)) as archive:
                for name in archive.namelist():
                    for row in csv.reader(io.TextIOWrapper(archive.open(name))):
                        if not row or not row[0].isdigit():
                            continue
                        r=normalize(row)
                        if START<=r[0] and r[6]<END:
                            if r[0] in candles and candles[r[0]]!=r:
                                raise ValueError('Conflicting duplicate')
                            candles[r[0]]=r
            manifest['sources'].append(dict(url=url,sha256=actual))
            print(f'{symbol} {interval} {date} checksum OK',flush=True)
        except Exception as e:
            manifest['errors'].append(dict(url=url,error=type(e).__name__+': '+str(e)))
    # Complete today's closed candles using the official public API.
    cursor=int(dt.datetime(2026,10,2,tzinfo=dt.timezone.utc).timestamp()*1000)
    try:
        while cursor<END:
            url=f'https://data-api.binance.vision/api/v3/klines?symbol={symbol}&interval={interval}&startTime={cursor}&endTime={END-1}&limit=1000'
            blob=get(url); rows=json.loads(blob)
            if not rows:
                break
            manifest['sources'].append(dict(url=url,sha256=hashlib.sha256(blob).hexdigest()))
            for row in rows:
                r=normalize(row)
                if r[6]<END:
                    candles[r[0]]=r
            next_cursor=timestamp(rows[-1][0])+STEPS[interval]
            if next_cursor<=cursor:
                raise ValueError('Pagination stalled')
            cursor=next_cursor
    except Exception as e:
        manifest['errors'].append(dict(stage='today_api',error=str(e)))
    path=output/f'{symbol}_{interval}.csv'
    with path.open('w',newline='') as f:
        w=csv.writer(f);w.writerow(['openTime','open','high','low','close','volume','closeTime'])
        w.writerows(candles[t] for t in sorted(candles))
    missing=[]; begin=None
    expected_end = END - END % STEPS[interval]
    for t in range(START,expected_end,STEPS[interval]):
        if t not in candles and begin is None:
            begin=t
        if t in candles and begin is not None:
            missing.append([begin,t]);begin=None
    if begin is not None:
        missing.append([begin,expected_end])
    manifest.update(count=len(candles),first=min(candles) if candles else None,
                    last=max(candles) if candles else None,missing_ranges=missing,
                    csv_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                    status='COMPLETE' if not missing else 'INCOMPLETE_REQUIRES_REVIEW')
    (output/f'{symbol}_{interval}_manifest.json').write_text(json.dumps(manifest,indent=2))
    print(json.dumps({k:v for k,v in manifest.items() if k not in ('sources','errors')},indent=2))
    if not candles:
        raise RuntimeError('No verified candles obtained')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--symbol',choices=['ADAUSDT','BTCUSDT'],required=True)
    p.add_argument('--interval',choices=list(STEPS),required=True);p.add_argument('--output',required=True)
    a=p.parse_args();run(a.symbol,a.interval,Path(a.output))
