import copy
import csv
import hashlib
import json
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

from execution_costs import START, STEP, digest, evaluate, net_return, prices, simulate, verify_frozen, main, attach_price_evidence


class ExecutionCostsTests(unittest.TestCase):
    def setUp(self):
        self.event = dict(type='DECISION', id='TRX-4h-future', symbol='TRX',
                          recorded_at=START + 1000, first_open=START + STEP,
                          horizon_end=START + 3 * STEP - 1, entry=100., target_pct=1.,
                          original=dict(accepted=True, direction='SELL'),
                          alternative=dict(accepted=True, direction='BUY'))
        self.receipt = dict(event_sha256=digest(self.event), confirmed_at=START + 2000,
                            commit='test-only')
        self.bars = {START + i * STEP: dict(openTime=START + i * STEP,
                                           closeTime=START + (i + 1) * STEP - 1,
                                           open=100., high=100.5, low=99.5, close=100.)
                     for i in (1, 2)}
        self.now = START + 3 * STEP

    def run_case(self, engine='alternative', **kwargs):
        return simulate(kwargs.get('event', self.event), kwargs.get('receipt', self.receipt),
                        engine, kwargs.get('bars', self.bars), kwargs.get('now', self.now))

    def test_pre_registration_is_excluded_even_if_profitable(self):
        event = dict(self.event, recorded_at=START - 1)
        self.assertEqual(self.run_case(event=event)['status'], 'EXCLUDED_PRE_REGISTRATION')

    def test_missing_tampered_and_late_receipts_cannot_trade(self):
        for receipt in (None, dict(self.receipt, event_sha256='wrong'),
                        dict(self.receipt, confirmed_at=self.event['first_open']),
                        dict(self.receipt, confirmed_at=START)):
            self.assertEqual(self.run_case(receipt=receipt)['status'], 'PENDING_DATA')

    def test_wait_for_entire_horizon_even_after_target_touched(self):
        self.bars[START + STEP]['high'] = 102.
        self.assertEqual(self.run_case(now=self.event['horizon_end'])['status'], 'PENDING_DATA')
        self.assertEqual(self.run_case()['reason'], 'TARGET')

    def test_gap_cannot_be_filled_from_other_bars(self):
        del self.bars[START + 2 * STEP]
        self.assertEqual(self.run_case()['reason'], 'MISSING_OFFICIAL_5M')

    def test_entry_already_beyond_barrier_is_not_retrospective_win(self):
        self.bars[START + STEP]['open'] = 102.
        self.assertEqual(self.run_case()['status'], 'NOT_EXECUTABLE_AT_REGISTERED_LEVELS')

    def test_same_bar_double_touch_returns_bounds_for_both_directions(self):
        self.bars[START + STEP].update(high=102., low=98.)
        for engine in ('original', 'alternative'):
            result = self.run_case(engine)
            self.assertEqual(result['status'], 'SIMULATED_INTERVAL')
            self.assertLess(result['scenarios'][1]['return_low'], 0)
            self.assertGreater(result['scenarios'][1]['return_high'], 0)

    def test_stop_opening_gap_uses_adverse_open_for_long_and_short(self):
        second = self.bars[START + 2 * STEP]
        second.update(open=98., high=99., low=97., close=98.)
        self.assertEqual(self.run_case()['exit_prices'], [98.])
        self.assertEqual(self.run_case()['reason'], 'STOP_GAP')
        second.update(open=102., high=103., low=101., close=102.)
        self.assertEqual(self.run_case('original')['exit_prices'], [102.])

    def test_target_gap_is_capped_and_horizon_exit_is_explicit(self):
        self.assertEqual(self.run_case()['reason'], 'HORIZON_CLOSE')
        self.bars[START + 2 * STEP].update(open=102., high=103., low=101., close=102.)
        result = self.run_case()
        self.assertEqual(result['reason'], 'TARGET_GAP_CAPPED')
        self.assertEqual(result['exit_prices'], [101.])

    def test_costs_reduce_returns_on_both_sides(self):
        for direction, exit_price in (('BUY', 101.), ('SELL', 99.)):
            self.assertLess(net_return(100., exit_price, direction, .002),
                            net_return(100., exit_price, direction, .001))
            self.assertLess(net_return(100., exit_price, direction, .001), .01)
        self.assertAlmostEqual(net_return(100., 100., 'BUY', 0), -.002)

    def test_admission_stays_locked_even_after_early_exit(self):
        second = copy.deepcopy(self.event)
        second.update(id='TRX-1h-future', recorded_at=START + STEP + 1000,
                      first_open=START + 2 * STEP, horizon_end=START + 4 * STEP - 1)
        self.bars[START + STEP]['high'] = 102.
        rows = evaluate([second, self.event], {self.event['id']: self.receipt},
                        {'TRX': self.bars}, self.now)['rows']
        omitted = [r for r in rows if r['id'] == second['id']]
        self.assertEqual([r['status'] for r in omitted], ['OMITTED_ACTIVE_WINDOW'] * 2)

    def test_duplicate_ids_fail_instead_of_double_counting(self):
        with self.assertRaises(ValueError):
            evaluate([self.event, self.event], {}, {}, self.now)

    def test_evidence_replays_resolved_result_without_changing_it(self):
        self.bars[START + STEP]['high'] = 102.
        original = self.run_case()
        result = dict(rows=[copy.deepcopy(original)])
        manifest = dict(inputs={'TRXUSDT_5m.csv': dict(sha256='source-fixture')})
        attach_price_evidence(result, [self.event], {'TRX': self.bars}, manifest)
        row = result['rows'][0]
        evidence = row.pop('price_evidence')
        self.assertEqual(row, original)
        self.assertEqual(len(evidence['bars']), 2)
        normalized = json.dumps(evidence['bars'], sort_keys=True, separators=(',', ':'))
        self.assertEqual(evidence['normalized_window_sha256'], hashlib.sha256(normalized.encode()).hexdigest())
        reconstructed = {bar['openTime']: bar for bar in evidence['bars']}
        self.assertEqual(self.run_case(bars=reconstructed), original)

    def test_not_executable_evidence_includes_actual_open_and_complete_horizon(self):
        self.bars[START + STEP]['open'] = 102.
        result = dict(rows=[self.run_case()])
        manifest = dict(inputs={'TRXUSDT_5m.csv': dict(sha256='fixture')})
        attach_price_evidence(result, [self.event], {'TRX': self.bars}, manifest)
        self.assertEqual(result['rows'][0]['status'], 'NOT_EXECUTABLE_AT_REGISTERED_LEVELS')
        self.assertEqual(result['rows'][0]['price_evidence']['bars'][0]['open'], 102.)

    def test_evidence_never_fills_gaps_or_adds_prices_to_pending_rows(self):
        pending = dict(rows=[self.run_case(now=self.event['horizon_end'])])
        attach_price_evidence(pending, [self.event], {}, {})
        self.assertNotIn('price_evidence', pending['rows'][0])
        resolved = dict(rows=[self.run_case()])
        del self.bars[START + 2 * STEP]
        with self.assertRaises(ValueError):
            attach_price_evidence(resolved, [self.event], {'TRX': self.bars}, {})

    def test_freeze_rejects_changed_engine_protocol_or_model(self):
        event = copy.deepcopy(self.event)
        event.update(timeframe='4h', engine_sha256='engine', protocol_sha256='protocol')
        event['alternative']['model_sha256'] = 'model'
        lock = dict(engine_sha256='engine', protocol_sha256='protocol',
                    models={'TRX-4h': 'model'})
        verify_frozen([event], lock)
        for key in ('engine_sha256', 'protocol_sha256'):
            with self.assertRaises(ValueError):
                verify_frozen([dict(event, **{key: 'changed'})], lock)
        event['alternative']['model_sha256'] = 'changed'
        with self.assertRaises(ValueError):
            verify_frozen([event], lock)

    def test_xrp_single_model_freeze_is_supported(self):
        event = copy.deepcopy(self.event)
        event.update(symbol='XRP', timeframe='30m', engine_sha256='e', protocol_sha256='p')
        event['alternative']['model_sha256'] = 'm'
        verify_frozen([event], dict(engine_sha256='e', protocol_sha256='p', model_sha256='m'))

    def test_cli_combines_months_and_records_input_hashes(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            older = dict(self.event, recorded_at=START - 1)
            first, second = root / 'events-09.jsonl', root / 'events-10.jsonl'
            first.write_text(json.dumps(older) + '\n')
            second.write_text('')
            receipts, lock, output = root / 'receipts.json', root / 'lock.json', root / 'out.json'
            receipts.write_text('{}')
            lock.write_text('{}')
            (root / 'manifest.json').write_text(json.dumps(dict(
                source='OFFICIAL_BINANCE_NATIVE_API', inputs={}, cutoff=START)))
            argv = ['execution_costs', '--events', str(first), str(second),
                    '--receipts', str(receipts), '--freeze-lock', str(lock),
                    '--data', str(root), '--output', str(output)]
            with patch('sys.argv', argv):
                main()
            result = json.loads(output.read_text())
            self.assertEqual(result['eligible_decisions'], 0)
            self.assertEqual(result['rows'], [])
            self.assertEqual(len(result['event_journals']), 2)
            self.assertEqual(result['market_cutoff'], START)
            self.assertEqual(result['receipts_sha256'], hashlib.sha256(b'{}').hexdigest())

    def test_collection_hooks_preserve_registration_before_costs(self):
        for cohort in ('selected', 'xrp'):
            script = Path(__file__).with_name('run_' + cohort + '_collection.sh').read_text()
            self.assertLess(script.index('periodic_capture.py'), script.index('execution_costs.py'))
            self.assertIn('events-*.jsonl', script)
            self.assertLess(script.index('execution_costs.py'), script.index('from ' + cohort + '_periodic_capture import persist'))

    def test_comparison_pairs_same_id_and_retains_original_outcome(self):
        self.bars[START + STEP]['high'] = 102.
        source = dict(type='OUTCOME', id=self.event['id'], engine='original',
                      status='FAIL', mfe=.5, mae=-1., window_candle_sha256='fixture')
        result = evaluate([self.event, source], {self.event['id']: self.receipt},
                          {'TRX': self.bars}, self.now)
        self.assertEqual(len(result['strictly_paired_admitted']), 1)
        self.assertGreater(result['strictly_paired_admitted'][0]['difference_low'], 0)
        original = next(r for r in result['rows'] if r['engine'] == 'original')
        self.assertEqual(original['source_directional_outcome']['status'], 'FAIL')
        self.assertEqual(source['status'], 'FAIL')

    def test_official_checksum_and_duplicate_candle_validation(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'official.csv'
            columns = ['openTime', 'open', 'high', 'low', 'close', 'closeTime']
            with path.open('w', newline='') as stream:
                writer = csv.DictWriter(stream, fieldnames=columns)
                writer.writeheader()
                writer.writerows(self.bars.values())
            sha = hashlib.sha256(path.read_bytes()).hexdigest()
            self.assertEqual(len(prices(path, sha)), 2)
            with self.assertRaises(ValueError):
                prices(path, 'bad')
            with path.open('a') as stream:
                writer = csv.DictWriter(stream, fieldnames=columns)
                writer.writerow(next(iter(self.bars.values())))
            with self.assertRaises(ValueError):
                prices(path, hashlib.sha256(path.read_bytes()).hexdigest())


if __name__ == '__main__':
    unittest.main()
