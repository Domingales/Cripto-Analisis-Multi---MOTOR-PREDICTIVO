"""Offline integrity/coverage audit of the frozen expanded-history ZIPs.

This is data validation, not a prediction experiment or independent validation.
"""
import argparse
import datetime as dt
import hashlib
import io
import json
import zipfile
from pathlib import Path
import numpy as np
from expanded_history import INTERVALS
TRAIN_END = 1680307200000  # Frozen protocol: 2023-04-01 UTC
TEST_START = 1696204800000  # Frozen protocol: 2023-10-02 UTC
from download_long_history import START, END
import download_long_history as history

STEPS = {'5m': 300000, **INTERVALS}


def gaps(times, step):
    """Missing aligned buckets, including listing prefix and retired-symbol suffix."""
    boundary = END - END % step
    full = np.arange(START, boundary, step, dtype=np.int64)
    missing = np.setdiff1d(full, times.astype(np.int64), assume_unique=True)
    if not len(missing):
        return []
    split = np.flatnonzero(np.diff(missing) != step) + 1
    return [[int(g[0]), int(g[-1]+step)] for g in np.split(missing, split)]


def validate(blob, manifest, interval):
    if hashlib.sha256(blob).hexdigest() != manifest['csv_sha256']:
        raise ValueError('CSV hash mismatch')
    a = np.loadtxt(io.BytesIO(blob), delimiter=',', skiprows=1, ndmin=2)
    step = STEPS[interval]
    if a.shape != (manifest['count'], 7) or not len(a) or not np.isfinite(a).all():
        raise ValueError('Invalid shape/finite values')
    if (np.any(np.diff(a[:, 0]) <= 0) or np.any(a[:, 0] % step) or
            np.any(a[:, 6] != a[:, 0]+step-1) or np.any(a[:, 0] < START) or
            np.any(a[:, 6] >= END)):
        raise ValueError('Invalid times')
    if (np.any(a[:, 1:5] <= 0) or np.any(a[:, 5] < 0) or
            np.any(a[:, 3] > np.minimum(a[:, 1], a[:, 4])) or
            np.any(a[:, 2] < np.maximum(a[:, 1], a[:, 4])) or np.any(a[:, 3] > a[:, 2])):
        raise ValueError('Invalid OHLCV')
    missing = gaps(a[:, 0], step)
    if (manifest['start'] != START or manifest['end'] != END or
            manifest['first'] != int(a[0, 0]) or manifest['last'] != int(a[-1, 0]) or
            manifest['missing_ranges'] != missing or
            manifest['missing_candles'] != sum((b-a)//step for a,b in missing)):
        raise ValueError('Manifest coverage mismatch')
    return a


def verify_aggregation(fine, derived, step):
    """Independently use array group boundaries; omit every incomplete group."""
    keys = (fine[:, 0].astype(np.int64) // step) * step
    starts = np.r_[0, np.flatnonzero(np.diff(keys)) + 1]
    ends = np.r_[starts[1:], len(fine)]
    good = ((ends-starts == step//300000) & (fine[starts, 0] == keys[starts]) &
            (fine[ends-1, 0] == keys[starts]+step-300000))
    starts, ends = starts[good], ends[good]
    expected = np.column_stack((fine[starts, 0], fine[starts, 1],
        [np.max(fine[s:e, 2]) for s,e in zip(starts,ends)],
        [np.min(fine[s:e, 3]) for s,e in zip(starts,ends)], fine[ends-1, 4],
        [np.sum(fine[s:e, 5]) for s,e in zip(starts,ends)], fine[starts, 0]+step-1))
    if expected.shape != derived.shape or not np.array_equal(expected[:, [0,6]], derived[:, [0,6]]):
        raise ValueError('Aggregation times/count mismatch')
    if not np.allclose(expected[:, 1:6], derived[:, 1:6], rtol=1e-12, atol=1e-9):
        raise ValueError('Aggregation OHLCV mismatch')


def audit(path):
    with zipfile.ZipFile(path) as z:
        coverage = json.loads(z.read('coverage.json'))
        symbol = coverage[0]['symbol']
        if len(coverage) != 6 or {m['interval'] for m in coverage} != set(STEPS):
            raise ValueError('Missing series')
        by_tf = {m['interval']: m for m in coverage}
        sources = z.read(symbol+'_sources.json')
        if hashlib.sha256(sources).hexdigest() != by_tf['5m']['origin']['sources_sha256']:
            raise ValueError('Sources manifest hash mismatch')
        provenance = json.loads(sources)
        if provenance['symbol'] != symbol or provenance['interval'] != '5m':
            raise ValueError('Provenance symbol mismatch')
        if any(not s['url'].startswith(('https://data.binance.vision/', 'https://data-api.binance.vision/'))
               for s in provenance['sources']):
            raise ValueError('Nonofficial source')
        arrays = {}
        report = []
        for tf, m in by_tf.items():
            if m != json.loads(z.read(f'{symbol}_{tf}_manifest.json')):
                raise ValueError('Coverage/series manifest mismatch')
            arrays[tf] = validate(z.read(f'{symbol}_{tf}.csv'), m, tf)
        for tf, m in by_tf.items():
            a = arrays[tf]; step = STEPS[tf]
            if tf != '5m':
                if m['origin']['parent_csv_sha256'] != by_tf['5m']['csv_sha256']:
                    raise ValueError('Parent hash mismatch')
                verify_aggregation(arrays['5m'], a, step)
            missing = m['missing_ranges']
            internal = [[x,y] for x,y in missing if x > m['first'] and y <= m['last']]
            horizon = (336 if tf == '1d' else 72 if tf == '4h' else 24)*3600000
            report.append(dict(symbol=symbol, interval=tf, count=len(a), first=m['first'], last=m['last'],
                csv_sha256=m['csv_sha256'], missing_candles=m['missing_candles'],
                internal_gap_ranges=len(internal), internal_missing=sum((y-x)//step for x,y in internal),
                prefix_missing=max(0,(m['first']-START)//step),
                suffix_missing=max(0,(END-END%step-m['last']-step)//step),
                train_candles=int(np.sum(a[:,6]+horizon < TRAIN_END)),
                calibration_candles=int(np.sum((a[:,6] >= TRAIN_END) & (a[:,6]+horizon < TEST_START-horizon))),
                examination_candles=int(np.sum(a[:,6] >= TEST_START)),
                status='INTEGRITY_AND_5M_AGGREGATION_PASS_OFFICIAL_INTERVAL_PARITY_PENDING'))
        return dict(symbol=symbol, archive_sha256=hashlib.sha256(Path(path).read_bytes()).hexdigest(),
            source_errors=len(provenance['errors']), source_anomalies=len(provenance['anomalies']), series=report)


def official_parity(path):
    """Compare fixed days against independently published official interval files."""
    results=[]
    with zipfile.ZipFile(path) as z:
        symbol=json.loads(z.read('coverage.json'))[0]['symbol']
        for tf,step in INTERVALS.items():
            history.STEPS[tf]=step
            a=np.loadtxt(io.BytesIO(z.read(f'{symbol}_{tf}.csv')),delimiter=',',skiprows=1,ndmin=2)
            for date in ('2023-10-03','2026-09-01'):
                start=int(dt.datetime.fromisoformat(date).replace(tzinfo=dt.timezone.utc).timestamp()*1000)
                derived=a[(a[:,0]>=start)&(a[:,0]<start+86400000)]
                record=dict(symbol=symbol,interval=tf,date=date)
                if not len(derived):
                    results.append({**record,'status':'NO_DERIVED_DATA_FOR_FIXED_DAY'})
                    continue
                manifest=dict(sources=[],errors=[],anomalies=[])
                rows={}
                if not history.archive(symbol,tf,'daily',date,rows,manifest):
                    results.append({**record,'status':'OFFICIAL_FILE_UNAVAILABLE','provenance':manifest})
                    continue
                compared=0
                for row in derived:
                    official=rows.get(int(row[0]))
                    if official is None or not np.allclose(row,np.array(official,dtype=float),rtol=0,atol=0):
                        # Only floating point summation volume gets a narrow tolerance.
                        if (official is None or not np.array_equal(row[[0,1,2,3,4,6]],np.array(official,dtype=float)[[0,1,2,3,4,6]]) or
                            not np.isclose(row[5],float(official[5]),rtol=1e-12,atol=1e-9)):
                            raise ValueError(f'Official parity mismatch: {symbol} {tf} {date} {row[0]}')
                    compared+=1
                omitted=sorted(set(rows)-set(map(int,derived[:,0])))
                results.append({**record,'status':'MATCH_ON_ALL_DERIVED_CANDLES','compared':compared,
                    'official_buckets_omitted_from_derived':omitted,'provenance':manifest})
    return results


if __name__ == '__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--archives', nargs='+', required=True)
    p.add_argument('--output', required=True)
    p.add_argument('--official-parity', action='store_true')
    args=p.parse_args()
    reports=[]
    for path in args.archives:
        result=audit(path); reports.append(result)
        if args.official_parity:
            result['official_interval_parity']=official_parity(path)
        print(result['symbol']+' integrity/aggregation PASS', flush=True)
    Path(args.output).write_text(json.dumps(dict(status='DATA_AUDIT_NOT_PREDICTIVE_RESULTS',
        historical_run=37224269151, assets=len(reports), reports=reports,
        limitations=['Official interval parity remains pending.',
          'Preparation candle counts are not usable training labels or model eligibility.',
          'Context lookbacks and all outcome gaps must be checked during replay.',
          'No predictive comparison or production change.']), indent=2)+'\n')
