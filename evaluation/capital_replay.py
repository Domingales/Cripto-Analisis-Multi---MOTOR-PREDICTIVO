"""Long-only cash/spot account; signal chronology, not hit labels, drives trades."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
from execution_costs import prices
from frozen_engine_digest import engine_digest

START = 1672531200000  # 2023-01-01 UTC
END = 1790953200000    # fixed verified archive, 2026-10-02 15:00 UTC
STEP = 300000
FEE = .001
SLIP = .0005


def simulate(signals, bars, start=START, end=END, fraction=.25, fee=FEE,
             slip=SLIP, no_loss=False, buy_hold=False):
    if not (0 < fraction <= 1 and 0 <= fee < 1 and 0 <= slip < 1):
        raise ValueError('Invalid execution parameters')
    cash, units, basis, fees, realized = 100., 0., 0., 0., 0.
    trades, skips, equity = [], [], []
    orders = {}
    last = -1
    for s in signals:
        t = s['time']
        if t <= last or s['direction'] not in ('BUY', 'SELL'):
            raise ValueError('Unordered/duplicate/invalid signals')
        last = t
        if not start <= t < end:
            continue
        # Even one millisecond of delay after the close misses the next open.
        # Use one complete 5m latency, rather than an unattainable same-time fill.
        execution = ((t // STEP) + 2)*STEP
        if execution >= end or execution not in bars:
            skips.append(dict(time=t, reason='NO_EXACT_EXECUTION_BAR'))
        else:
            orders[execution] = s
    market = [(t,b) for t,b in sorted(bars.items()) if start <= t < end]
    if not market:
        raise ValueError('No prices')
    if buy_hold:
        orders = {market[0][0]: dict(time=start-1, direction='BUY')}
        skips = []
    peak, drawdown, missing = 100., 0., 0
    previous = start-STEP
    for t, b in market:
        missing += max(0, (t-previous)//STEP-1)
        previous = t
        s = orders.get(t)
        if s:
            side = s['direction']
            if side == 'BUY' and units == 0:
                budget = cash*fraction
                if budget < 5:
                    skips.append(dict(time=s['time'], reason='BELOW_ASSUMED_5_UNIT_MINIMUM'))
                else:
                    fill = b['open']*(1+slip)
                    notional = budget/(1+fee)
                    commission = notional*fee
                    units = notional/fill
                    cash -= budget; basis = budget; fees += commission
                    trades.append(dict(signal_time=s['time'], execution_time=t, side=side,
                                       fill=fill, units=units, fee=commission, cash=cash))
            elif side == 'SELL' and units > 0:
                fill = b['open']*(1-slip)
                proceeds = units*fill*(1-fee)
                if no_loss and proceeds < basis:
                    skips.append(dict(time=s['time'], reason='NO_LOSS_RULE_LOCKS_POSITION'))
                else:
                    commission = units*fill*fee
                    pnl = proceeds-basis
                    cash += proceeds; realized += pnl; fees += commission
                    trades.append(dict(signal_time=s['time'], execution_time=t, side=side,
                                       fill=fill, units=units, fee=commission, pnl=pnl, cash=cash))
                    units, basis = 0., 0.
            else:
                skips.append(dict(time=s['time'], reason='ALREADY_HOLDING' if side == 'BUY' else 'NO_POSITION'))
        value = cash+units*b['close']
        peak = max(peak, value)
        drawdown = max(drawdown, 1-value/peak)
        if cash < -1e-8 or units < 0:
            raise ValueError('Cash conservation violated')
        # Daily observations plus final bar; drawdown measured at every observed 5m close.
        if (t+STEP) % 86400000 == 0 or t == market[-1][0]:
            equity.append(dict(time=b['closeTime'], equity=value, cash=cash, units=units))
    final = market[-1][1]
    value = cash+units*final['close']
    liquidation = cash+units*final['close']*(1-slip)*(1-fee)
    closed = [x for x in trades if x['side'] == 'SELL']
    return dict(initial=100., final_marked_value=value, hypothetical_net_liquidation=liquidation,
                return_pct=(value/100-1)*100, cash=cash, open_units=units,
                open_position_cost=basis, unrealized_pnl=units*final['close']-basis,
                realized_pnl=realized, fees_paid=fees, max_drawdown_pct=100*drawdown,
                buys=sum(x['side']=='BUY' for x in trades), sells=len(closed),
                winning_closed_trades=sum(x['pnl']>0 for x in closed),
                losing_closed_trades=sum(x['pnl']<0 for x in closed),
                observed_5m_bars=len(market), missing_5m_bars=missing,
                last_mark_time=final['closeTime'], trades=trades, skipped=skips, daily_equity=equity)


def run(data, source, output):
    data, source, output = Path(data), Path(source), Path(output)
    digest = engine_digest(Path(__file__).resolve().parents[1])
    if digest != 'f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7':
        raise ValueError('Predictor changed')
    manifest = json.loads((data/'XRPUSDT_5m_manifest.json').read_text())
    expected = json.loads((source/'magnitude-output/prelabel-manifest.json').read_text())['inputs']['XRPUSDT_5m']
    if manifest['csv_sha256'] != expected:
        raise ValueError('Prices differ from replay lineage')
    bars = prices(data/'XRPUSDT_5m.csv', expected)
    with (source/'replay-kotlin/kotlin_accepted.csv').open() as f:
        accepted = {int(r['signalCloseTime']) for r in csv.DictReader(f)}
    signals = []
    with (source/'replay-kotlin/kotlin_decisions.csv').open() as f:
        for r in csv.DictReader(f):
            t = int(r['signalCloseTime'])
            if t in accepted:
                if r['isSignal'] != 'true' or r['direction'] == 'WAIT':
                    raise ValueError('Accepted decision mismatch')
                signals.append(dict(time=t, direction='SELL' if 'SELL' in r['direction'] else 'BUY'))
    if len(signals) != len(accepted):
        raise ValueError('Missing accepted decisions')
    scenarios = {}
    for name, kwargs in [('main_25pct',{}), ('no_loss_25pct',dict(no_loss=True)),
                         ('alerts_100pct',dict(fraction=1)),
                         ('no_loss_100pct',dict(no_loss=True,fraction=1)),
                         ('cost_stress_100pct',dict(fee=.002,slip=.002,fraction=1)),
                         ('cash',None), ('buy_hold_25pct',dict(buy_hold=True)),
                         ('buy_hold_100pct',dict(buy_hold=True, fraction=1)),
                         ('cost_stress_25pct',dict(fee=.002,slip=.002))]:
        scenarios[name] = simulate([] if kwargs is None else signals, bars, **(kwargs or {}))
    output.mkdir(parents=True, exist_ok=True)
    compact = {}
    for name, result in scenarios.items():
        for key in ('trades','skipped','daily_equity'):
            (output/f'{name}-{key}.jsonl').write_text(''.join(json.dumps(x)+'\n' for x in result[key]))
        compact[name] = {k:v for k,v in result.items() if k not in ('trades','skipped','daily_equity')}
        compact[name]['skipped_by_reason'] = {reason:sum(x['reason']==reason for x in result['skipped'])
                                             for reason in sorted({x['reason'] for x in result['skipped']})}
    report = dict(start=START,end=END,symbol='XRPUSDT',timeframe='30m',
                  unit='NORMALIZED_USDT_NOT_ACTUAL_EUR', predictor_sha256=digest,
                  prices_sha256=expected, scenarios=compact, production_changed=False,
                  assumptions=dict(fee_per_side=FEE,slippage_per_side=SLIP,default_fraction=.25,
                    all_in_scenarios=['alerts_100pct','no_loss_100pct','cost_stress_100pct'],
                    execution_latency='first full 5m open strictly after alert plus processing margin',
                    minimum_order=5, leverage=False, margin=False, tax=False),
                  limits=['Not current date: verified archive ends2026-10-02.',
                    '100 normalized USDT units, not a EUR balance: FX and USDT/USD basis excluded.',
                    'Original as-of pattern learning before each signal; no 2023-fitted alternative reused in2020.',
                    'Threshold81 is confidence/filters, not certified81% probability.',
                    'Fractional ideal spot sizing: historical lot sizes/minima/depth not reconstructed.',
                    'Costs and latency are fixed scenarios, not verified historical exchange tariffs.',
                    'All sell alerts close main strategy even at a loss; separate no-loss variant may remain locked.',
                    'Missing exact execution bars cancel orders; drawdown only at observed5m closes.',
                    'Not a new independent holdout, recommendation, or real trading.'])
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))
    return report


if __name__ == '__main__':
    p=argparse.ArgumentParser()
    for name in ('data','source','output'): p.add_argument('--'+name,required=True)
    a=p.parse_args();run(a.data,a.source,a.output)
