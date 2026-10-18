"""Isolated future-only execution sensitivity; never changes signal labels.

Inputs are the durable research journal, its receipts and official native 5m
CSV files plus the fetch_selected_data manifest. No network or trading actions.
"""
import argparse
import base64
import csv
import hashlib
import json
import math
import re
import time
from pathlib import Path

START = 1792108800000  # 2026-10-16T00:00:00Z
STEP = 300000
FEE = .001
SLIPPAGES = (0., .001, .002)


def digest(event):
    return hashlib.sha256(json.dumps(event, sort_keys=True).encode()).hexdigest()


def verify_frozen(events, lock):
    for event in events:
        if event['type'] != 'DECISION' or event['recorded_at'] < START:
            continue
        for key in ('engine_sha256', 'protocol_sha256'):
            if not lock.get(key) or event.get(key) != lock[key]:
                raise ValueError('Frozen ' + key + ' mismatch')
        model = lock.get('models', {}).get(event['symbol'] + '-' + event['timeframe'],
                                         lock.get('model_sha256'))
        if not model or event['alternative'].get('model_sha256') != model:
            raise ValueError('Frozen model mismatch')


def prices(path, expected_hash):
    raw = Path(path).read_bytes()
    if hashlib.sha256(raw).hexdigest() != expected_hash:
        raise ValueError('Official input checksum mismatch')
    bars = {}
    with Path(path).open(newline='') as source:
        for row in csv.DictReader(source):
            opening = int(row['openTime'])
            bar = {k: float(row[k]) for k in ('open', 'high', 'low', 'close')}
            bar.update(openTime=opening, closeTime=int(row['closeTime']))
            if (opening % STEP or bar['closeTime'] != opening + STEP - 1
                    or not all(math.isfinite(bar[k]) and bar[k] > 0
                               for k in ('open', 'high', 'low', 'close'))
                    or not bar['low'] <= min(bar['open'], bar['close'])
                    <= max(bar['open'], bar['close']) <= bar['high']
                    or opening in bars):
                raise ValueError('Invalid or duplicate official 5m bar')
            bars[opening] = bar
    return bars


def net_return(entry, exit_price, direction, slippage):
    sign = 1 if direction == 'BUY' else -1
    entry_fill = entry * (1 + sign * slippage)
    exit_fill = exit_price * (1 - sign * slippage)
    # Constant entry notional, fees on each traded amount, no compounding.
    gross = sign * (exit_fill - entry_fill) / entry_fill
    return gross - FEE * (1 + exit_fill / entry_fill)


def simulate(event, receipt, engine, bars, now):
    """Return a complete-window result or explicit non-evaluable state."""
    result = dict(id=event['id'], symbol=event['symbol'], timeframe=event.get('timeframe'),
                  engine=engine, event_sha256=digest(event))
    if event['recorded_at'] < START:
        return dict(result, status='EXCLUDED_PRE_REGISTRATION')
    if not event[engine]['accepted']:
        return dict(result, status='NO_SIGNAL')
    first, end = event['first_open'], event['horizon_end']
    if (not receipt or receipt.get('event_sha256') != result['event_sha256']
            or not event['recorded_at'] <= receipt.get('confirmed_at', -1) < first
            or first % STEP or (end + 1) % STEP or end < first
            or first != (event['recorded_at'] // STEP + 1) * STEP):
        return dict(result, status='PENDING_DATA', reason='INVALID_DURABILITY_OR_WINDOW')
    result['receipt_commit'] = receipt.get('commit')
    if now <= end:
        return dict(result, status='PENDING_DATA', reason='WINDOW_NOT_CLOSED')
    window = [bars.get(t) for t in range(first, end + 1, STEP)]
    if not all(window):
        return dict(result, status='PENDING_DATA', reason='MISSING_OFFICIAL_5M')
    direction = event[engine]['direction']
    if direction not in ('BUY', 'SELL'):
        raise ValueError('Unknown direction')
    reference, target_pct = float(event['entry']), float(event['target_pct'])
    if (not math.isfinite(reference) or reference <= 0
            or not math.isfinite(target_pct) or not 0 < target_pct < 100):
        raise ValueError('Invalid frozen levels')
    upper, lower = reference * (1 + target_pct / 100), reference * (1 - target_pct / 100)
    entry = window[0]['open']
    if not lower < entry < upper:
        return dict(result, status='NOT_EXECUTABLE_AT_REGISTERED_LEVELS')
    target, stop = (upper, lower) if direction == 'BUY' else (lower, upper)
    result.update(direction=direction, entry=entry, first_open=first,
                  horizon_end=end, short_hypothetical=direction == 'SELL')
    exits, reason, exit_time = [window[-1]['close']], 'HORIZON_CLOSE', end
    for bar in window:
        op = bar['open']
        stop_gap = op <= stop if direction == 'BUY' else op >= stop
        target_gap = op >= target if direction == 'BUY' else op <= target
        stop_hit = bar['low'] <= stop if direction == 'BUY' else bar['high'] >= stop
        target_hit = bar['high'] >= target if direction == 'BUY' else bar['low'] <= target
        if stop_gap:
            exits, reason, exit_time = [op], 'STOP_GAP', bar['openTime']
        elif target_gap:
            exits, reason, exit_time = [target], 'TARGET_GAP_CAPPED', bar['openTime']
        elif stop_hit and target_hit:
            exits, reason, exit_time = [stop, target], 'AMBIGUOUS_BOTH_TOUCHED', bar['closeTime']
        elif stop_hit or target_hit:
            exits, reason, exit_time = [stop if stop_hit else target], ('STOP' if stop_hit else 'TARGET'), bar['closeTime']
        else:
            continue
        break
    scenarios = []
    for slip in SLIPPAGES:
        returns = sorted(net_return(entry, price, direction, slip) for price in exits)
        scenarios.append(dict(fee_per_side=FEE, slippage_per_side=slip,
                              main=slip == .001, return_low=returns[0], return_high=returns[-1]))
    return dict(result, status='SIMULATED_INTERVAL' if len(exits) == 2 else 'SIMULATED',
                exit_prices=exits, reason=reason, exit_time=exit_time, scenarios=scenarios)


def evaluate(events, receipts, barsets, now):
    decisions = [e for e in events if e['type'] == 'DECISION']
    if len({e['id'] for e in decisions}) != len(decisions):
        raise ValueError('Duplicate decision IDs')
    ordered = sorted(decisions, key=lambda e: (e['first_open'], e['recorded_at'], e['id']))
    outcomes = {(e['id'], e['engine']): e for e in events if e['type'] == 'OUTCOME'}
    locked, results = {}, []
    for event in ordered:
        if event['recorded_at'] < START:
            continue
        for engine in ('original', 'alternative'):
            if not event[engine]['accepted']:
                continue
            key = (event['symbol'], engine)
            if event['first_open'] <= locked.get(key, -1):
                results.append(dict(id=event['id'], symbol=event['symbol'],
                                    timeframe=event.get('timeframe'), engine=engine,
                                    status='OMITTED_ACTIVE_WINDOW'))
                continue
            # Admission uses only known times, never outcome or simulated return.
            locked[key] = event['horizon_end']
            row = simulate(event, receipts.get(event['id']), engine,
                           barsets.get(event['symbol'], {}), now)
            snapshot = base64.b64decode(event.get('context_snapshot_base64', '')).decode('utf-8')
            regime = re.search(r'\bregime=([A-Z_]+)', snapshot)
            row['regime_at_registration'] = regime.group(1) if regime else 'UNKNOWN'
            source_outcome = outcomes.get((event['id'], engine))
            row['source_directional_outcome'] = (
                {key: source_outcome.get(key) for key in ('status', 'mfe', 'mae', 'window_candle_sha256')}
                if source_outcome else None)
            row['mfe_mae_scope'] = 'SOURCE_FULL_WINDOW_AT_REGISTERED_REFERENCE_NOT_EXECUTED_PNL'
            results.append(row)
    counts = {}
    for engine in ('original', 'alternative'):
        statuses = {}
        for row in results:
            if row['engine'] == engine:
                statuses[row['status']] = statuses.get(row['status'], 0) + 1
        counts[engine] = statuses
    paired = []
    indexed = {(r['id'], r['engine']): r for r in results}
    for event in ordered:
        original = indexed.get((event['id'], 'original'))
        alternative = indexed.get((event['id'], 'alternative'))
        if not original or not alternative or not all(r['status'].startswith('SIMULATED')
                                                      for r in (original, alternative)):
            continue
        one, two = original['scenarios'][1], alternative['scenarios'][1]
        paired.append(dict(id=event['id'], original_return_low=one['return_low'],
                           original_return_high=one['return_high'],
                           alternative_return_low=two['return_low'],
                           alternative_return_high=two['return_high'],
                           difference_low=two['return_low'] - one['return_high'],
                           difference_high=two['return_high'] - one['return_low']))
    return dict(status='RESEARCH_SIMULATION_NOT_EXECUTABLE_PROFITABILITY',
                registered_start=START, fee_per_side=FEE, rows=results,
                eligible_decisions=sum(e['recorded_at'] >= START for e in decisions),
                counts=counts, strictly_paired_admitted=paired)


def attach_price_evidence(result, events, barsets, manifest):
    """Retain the normalized OHLC inputs used by every resolved admission.

    Evidence is additive: it never changes admission, outcome or cost policy.
    The complete horizon is retained even when a first touch occurred earlier.
    """
    decisions = {e['id']: e for e in events if e['type'] == 'DECISION'}
    for row in result['rows']:
        if row['status'] not in ('SIMULATED', 'SIMULATED_INTERVAL',
                                 'NOT_EXECUTABLE_AT_REGISTERED_LEVELS'):
            continue
        event = decisions[row['id']]
        opens = range(event['first_open'], event['horizon_end'] + 1, STEP)
        bars = barsets.get(event['symbol'], {})
        if any(t not in bars for t in opens):
            raise ValueError('Resolved result lacks complete price evidence')
        window = [dict(bars[t]) for t in opens]
        canonical = json.dumps(window, sort_keys=True, separators=(',', ':'), allow_nan=False)
        source = event['symbol'] + 'USDT_5m.csv'
        row['price_evidence'] = dict(
            schema=1, source_file=source,
            source_file_sha256=manifest['inputs'][source]['sha256'],
            first_open=event['first_open'], horizon_end=event['horizon_end'],
            normalization='PARSED_OHLC_MS_JSON_SORTED_KEYS_COMPACT_V1',
            normalized_window_sha256=hashlib.sha256(canonical.encode()).hexdigest(),
            bars=window)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--events', type=Path, nargs='+', required=True)
    for name in ('receipts', 'freeze-lock', 'data', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    manifest = json.loads((args.data / 'manifest.json').read_text())
    if manifest.get('source') != 'OFFICIAL_BINANCE_NATIVE_API':
        raise ValueError('Require the official native collector manifest')
    events = [json.loads(line) for path in args.events
              for line in path.read_text().splitlines() if line.strip()]
    verify_frozen(events, json.loads(args.freeze_lock.read_text()))
    receipts = json.loads(args.receipts.read_text())
    barsets = {}
    for symbol in {e['symbol'] for e in events if e['type'] == 'DECISION'}:
        name = symbol + 'USDT_5m.csv'
        if name in manifest['inputs']:
            barsets[symbol] = prices(args.data / name, manifest['inputs'][name]['sha256'])
    now = int(time.time() * 1000)
    result = evaluate(events, receipts, barsets, now)
    attach_price_evidence(result, events, barsets, manifest)
    result['evaluated_at'] = now
    result['market_cutoff'] = manifest['cutoff']
    result['event_journals'] = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                                for path in args.events}
    result['receipts_sha256'] = hashlib.sha256(args.receipts.read_bytes()).hexdigest()
    result['official_manifest_sha256'] = hashlib.sha256((args.data / 'manifest.json').read_bytes()).hexdigest()
    result['freeze_lock_sha256'] = hashlib.sha256(args.freeze_lock.read_bytes()).hexdigest()
    args.output.write_text(json.dumps(result, indent=2) + '\n')


if __name__ == '__main__':
    main()
