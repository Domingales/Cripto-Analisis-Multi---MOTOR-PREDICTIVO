#!/usr/bin/env python3
"""Append immutable predictions, resolve expired ones using complete Binance 5m."""
import argparse
import base64
import csv
import hashlib
import json
import pathlib
import time

STEP = 300000


def resolve(prediction, candles, now):
    if now <= prediction['horizon_end']:
        return None
    selected = [c for c in candles if prediction['first_open'] <= int(c['openTime'])
                and int(c['closeTime']) <= prediction['horizon_end']]
    expected = list(range(prediction['first_open'], prediction['horizon_end'] + 1, STEP))
    if ([int(c['openTime']) for c in selected] != expected or
            any(int(c['closeTime']) != int(c['openTime']) + STEP - 1 for c in selected)):
        return None  # missing data remains pending; never becomes a neutral
    entry, threshold = prediction['entry'], prediction['target_pct']
    sell = 'SELL' in prediction['direction']
    status, reason, timestamp = 'NEUTRAL', 'WINDOW_EXPIRED', prediction['horizon_end']
    mfe, mae = 0., 0.
    touched = False
    for c in selected:
        high, low = float(c['high']), float(c['low'])
        favorable = (entry - low if sell else high - entry) / entry * 100
        adverse = (entry - high if sell else low - entry) / entry * 100
        mfe, mae = max(mfe, favorable), min(mae, adverse)
        hit, fail = favorable >= threshold, adverse <= -threshold
        if not touched and (hit or fail):
            touched = True
            timestamp = int(c['closeTime'])
            status = 'NEUTRAL' if hit and fail else 'HIT' if hit else 'FAIL'
            reason = 'BOTH_TOUCHED_SAME_5M_CANDLE' if hit and fail else 'TARGET_FIRST' if hit else 'STOP_FIRST'
    return dict(status=status, reason=reason, outcome_time=timestamp, mfe=mfe, mae=mae,
                window_candle_sha256=hashlib.sha256(json.dumps(selected, sort_keys=True).encode()).hexdigest())


def append(path, record):
    with path.open('a', encoding='utf-8') as f:
        f.write(json.dumps(record, sort_keys=True, ensure_ascii=False) + '\n')


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--root', required=True)
    p.add_argument('--data', required=True)
    p.add_argument('--decision', required=True)
    p.add_argument('--version', required=True)
    args = p.parse_args()
    root, data = pathlib.Path(args.root), pathlib.Path(args.data)
    root.mkdir(parents=True, exist_ok=True)
    events = root / 'events.jsonl'
    history = [json.loads(s) for s in events.read_text().splitlines()] if events.exists() else []
    now = int(time.time() * 1000)
    fields = pathlib.Path(args.decision).read_text().strip().split('\t')
    close, recorded, accepted, direction, entry, target, first, end, snapshot = fields
    close, recorded, first, end = map(int, (close, recorded, first, end))
    if not 0 <= recorded - close <= 20 * 60000 or not 0 <= now - recorded <= 10 * 60000:
        raise ValueError('Decision stale: do not backfill it as prospective')
    base64.b64decode(snapshot, validate=True)
    identifier = f"ADA-1h-{close}-{args.version}"
    if not any(e.get('id') == identifier and e['type'] == 'DECISION' for e in history):
        event = dict(type='DECISION', id=identifier, version=args.version,
                     symbol='ADAUSDT', timeframe='1h', candle_close=close, recorded_at=recorded,
                     accepted=accepted == 'true', direction=direction, entry=float(entry),
                     target_pct=float(target), first_open=first, horizon_end=end,
                     context_snapshot_base64=snapshot, threshold=81, cooldown_minutes=60,
                     cohort='delayed_closed_candle_replay_45d', news='absent_neutral',
                     rules='symmetric_ATR_target_stop_first_touch_5m_v1',
                     data_manifest=json.loads((data / 'manifest.json').read_text()))
        append(events, event)
        history.append(event)
    with (data / 'ADA_5m.csv').open() as f:
        candles = list(csv.DictReader(f))
    resolved_ids = {e['id'] for e in history if e['type'] == 'OUTCOME'}
    for event in history:
        if event['type'] != 'DECISION' or not event['accepted'] or event['id'] in resolved_ids:
            continue
        result = resolve(event, candles, now)
        if result is not None:
            outcome = dict(type='OUTCOME', id=event['id'], evaluated_at=now, **result)
            append(events, outcome)
            resolved_ids.add(event['id'])
    (root / 'README.md').write_text(
        '# Registro prospectivo ADA 1h\n\n'
        'JSONL append-only: DECISION precede a OUTCOME. Sin rellenar huecos retrospectivamente. '
        'Sólo se toma la última vela cerrada, con retraso máximo de 20 minutos. '
        'Se registran también decisiones sin señal. Cada versión tiene su cohorte. '
        'La calibración se reconstruye sobre 45 días conocidos; no es la vigilancia del móvil. '
        'La evaluación empieza en la primera vela 5m completa posterior al registro, '
        'con precio de referencia de la vela 1h cerrada y horizonte de 24h. '
        'Por ello esta cohorte retrasada se informa por separado del backtest. '
        'Los periodos entre ejecuciones no son decisiones observadas. '
        'Los huecos 5m quedan pendientes. MFE/MAE corresponden al horizonte completo.\n')
    print(f"Decisiones: {sum(e['type'] == 'DECISION' for e in history)}; "
          f"resultados guardados: {len(resolved_ids)}")


if __name__ == '__main__':
    main()
