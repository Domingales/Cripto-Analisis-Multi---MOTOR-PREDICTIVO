"""Freeze prospective models on labels known at the historical 2026-10-02 cutoff."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
import expanded_replay as replay
from expanded_history import INTERVALS

PROTOCOL = Path(__file__).with_name('selected_validation_protocol.json')


def phase(t, duration, protocol):
    if t + duration < protocol['alternative_train_end']:
        return 'TRAIN'
    if protocol['alternative_train_end'] <= t and t + duration < protocol['alternative_calibration_label_end']:
        return 'CALIBRATION'
    return 'EXCLUDED'


def prepare(data, kotlin, output, symbol, tf, version):
    protocol = json.loads(PROTOCOL.read_text())
    if [symbol, tf] not in protocol['selections']:
        return
    output.mkdir(parents=True, exist_ok=True)
    duration = replay.horizon(tf)
    names = [(symbol+'USDT_'+k, INTERVALS[k]) for k in replay.contexts(tf)]
    if symbol != 'BTC': names.append(('BTCUSDT_4h', INTERVALS['4h']))
    arrays, manifests = {}, {}
    for name, step in names + [(symbol+'USDT_5m', 300000)]:
        arrays[name], manifests[name] = replay.read_series(data, name, step)
    fine = arrays.pop(symbol+'USDT_5m')
    features = replay.Features(arrays, dict(names))
    training = dict(TRAIN=[], CALIBRATION=[])
    with (kotlin/'kotlin_decisions.csv').open() as f:
        for row in csv.DictReader(f):
            t = int(row['signalCloseTime'])
            group = phase(t, duration, protocol)
            if group not in training: continue
            x = features.at(t)
            if x is None: continue
            labels = [replay.resolution(fine,t,float(row['entry']),float(row['targetPct']),side,duration)
                      for side in ('BUY','SELL')]
            training[group].append((t,x,labels))
    models, manifest = replay.freeze_model(training,duration,output,version,
        {n:m['csv_sha256'] for n,m in manifests.items()},list(arrays))
    # Override the historical experiment's dates with this independently declared preparation.
    manifest.update(train_end=protocol['alternative_train_end'],
        calibration_label_end=protocol['alternative_calibration_label_end'],
        examination_start=1791331200000, symbol=symbol, timeframe=tf,
        protocol_sha256=hashlib.sha256(PROTOCOL.read_bytes()).hexdigest(),
        cohort=protocol['cohort'], prepared_for='NEW_PROSPECTIVE_ONLY_NOT_2023_EXAMINATION',
        status='FROZEN_FOR_NEW_PROSPECTIVE_VALIDATION' if models else 'NOT_EVALUABLE_INSUFFICIENT_PROSPECTIVE_PREPARATION')
    (output/'model_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(symbol,tf,manifest['status'],manifest['counts'],flush=True)


if __name__ == '__main__':
    p=argparse.ArgumentParser()
    for key in ('data','kotlin','output'): p.add_argument('--'+key,type=Path,required=True)
    for key in ('symbol','timeframe','version'): p.add_argument('--'+key,required=True)
    a=p.parse_args(); prepare(a.data,a.kotlin,a.output,a.symbol,a.timeframe,a.version)
