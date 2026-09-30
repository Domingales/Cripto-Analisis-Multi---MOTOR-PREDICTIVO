#!/usr/bin/env python3
"""Descarga velas cerradas de Binance Spot para una auditoría ADA 1h reproducible."""
import csv
import datetime as dt
import json
import hashlib
import os
import pathlib
import time
import urllib.error
import urllib.parse
import urllib.request

UTC = dt.timezone.utc
end_value = os.environ.get('EVALUATION_END_UTC', '2026-09-29T00:00:00+00:00')
END = dt.datetime.fromisoformat(end_value.replace('Z', '+00:00')) if end_value else dt.datetime.now(UTC).replace(minute=0, second=0, microsecond=0)
if END.tzinfo is None or END.minute or END.second or END.microsecond:
    raise ValueError('EVALUATION_END_UTC debe ser una hora UTC cerrada')
END = END.astimezone(UTC)
END_MS = int(END.timestamp() * 1000)
OUT = pathlib.Path(os.environ.get('MARKET_DATA_DIR', 'evaluation/data'))
OUT.mkdir(parents=True, exist_ok=True)
SPECS = [('ADA', '1h', 45), ('ADA', '4h', 120), ('ADA', '1d', 350),
         ('BTC', '4h', 120), ('ADA', '5m', 45)]
STEPS = {'1h': 3600000, '4h': 14400000, '1d': 86400000, '5m': 300000}


def request(symbol, interval, cursor):
    query = urllib.parse.urlencode({'symbol': symbol + 'USDT', 'interval': interval,
                                    'startTime': cursor, 'endTime': END_MS - 1, 'limit': 1000})
    url = 'https://data-api.binance.vision/api/v3/klines?' + query
    for attempt in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'CriptoAnalisisMulti-Evaluation/1.0'}), timeout=20) as res:
                rows = json.load(res)
            if not isinstance(rows, list):
                raise ValueError('Binance no devolvió una lista de velas')
            return rows
        except (urllib.error.URLError, TimeoutError) as exc:
            if attempt == 2:
                raise RuntimeError(f'Binance no disponible ({symbol} {interval}): {exc}') from exc
            time.sleep(2 ** attempt)

for symbol, interval, days in SPECS:
    start_ms = (END_MS // STEPS[interval]) * STEPS[interval] - days * 86400000
    start = dt.datetime.fromtimestamp(start_ms / 1000, UTC)
    cursor = int(start.timestamp() * 1000)
    step = STEPS[interval]
    rows = []
    while cursor < END_MS:
        page = request(symbol, interval, cursor)
        if not page:
            break
        rows.extend(x for x in page if int(x[6]) < END_MS)
        next_cursor = int(page[-1][0]) + step
        if next_cursor <= cursor:
            raise ValueError(f'Paginación detenida: {symbol} {interval}')
        cursor = next_cursor
        time.sleep(0.12)
    if len(rows) < 240:
        raise ValueError(f'Histórico insuficiente: {symbol} {interval} {len(rows)} velas')
    expected = (END_MS - start_ms) // step
    if len(rows) != expected or int(rows[0][0]) != int(start.timestamp() * 1000) or int(rows[-1][6]) != start_ms + expected * step - 1:
        raise ValueError(f'Intervalo incompleto: {symbol} {interval}; esperadas {expected}, recibidas {len(rows)}')
    for old, new in zip(rows, rows[1:]):
        if int(new[0]) - int(old[0]) != step:
            raise ValueError(f'Hueco o duplicado en {symbol} {interval} cerca de {old[0]}')
    dest = OUT / f'{symbol}_{interval}.csv'
    with dest.open('w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerow(['openTime', 'open', 'high', 'low', 'close', 'volume', 'closeTime'])
        writer.writerows((x[0], x[1], x[2], x[3], x[4], x[5], x[6]) for x in rows)
    print(f'{symbol} {interval}: {len(rows)} velas cerradas, {rows[0][0]}..{rows[-1][6]}')
(OUT / 'manifest.json').write_text(json.dumps({'source': 'Binance Spot public API', 'endpoint': 'https://data-api.binance.vision/api/v3/klines', 'pair': 'ADAUSDT',
    'cutoff_utc': END.isoformat(), 'threshold': 81, 'days_primary': 45, 'timeframe': '1h', 'sha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(OUT.glob('*.csv'))}}, indent=2) + '\n')

