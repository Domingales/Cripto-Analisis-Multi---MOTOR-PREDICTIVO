import base64
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import magnitude_live as live
from frozen_engine_digest import engine_digest, DOMAIN, NAMES


class MagnitudeLiveTests(unittest.TestCase):
    def test_freeze_preserved_and_predictor_changes_still_detected(self):
        lock=json.loads(Path('evaluation/selected-prospective/freeze-lock.json').read_text())
        self.assertEqual(engine_digest(),lock['engine_sha256'])
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            for name in NAMES:
                p=root/(DOMAIN+name+'.kt');p.parent.mkdir(parents=True,exist_ok=True)
                p.write_bytes(Path(DOMAIN+name+'.kt').read_bytes())
            baseline=engine_digest(root)
            (root/(DOMAIN+'IndependentEvaluator.kt')).write_text('not a predictor input')
            self.assertEqual(baseline,engine_digest(root))
            with (root/(DOMAIN+'SignalEngine.kt')).open('a') as f:f.write('// modification')
            self.assertNotEqual(baseline,engine_digest(root))

    def test_four_percent_contract_uses_declared_range_before_outcome(self):
        snapshot=base64.b64encode(b'AnalysisResult(forecastLowPct=3.0, forecastHighPct=5.0)').decode()
        c=live.new_contract(snapshot,100)
        self.assertEqual(4.,c['target_pct'])
        self.assertEqual(100,c['issued_at'])
        self.assertEqual((live.DAY,2*live.DAY,5*live.DAY),tuple(c[k] for k in ('horizon_ms','grace_ms','expiry_ms')))
        self.assertEqual(.75,c['minimum_fraction'])
        self.assertIsNone(live.new_contract(base64.b64encode(b'legacy snapshot').decode(),100))

    def test_old_decisions_are_not_relabelled_and_receipts_are_required(self):
        snapshot=base64.b64encode(b'forecastLowPct=3.0, forecastHighPct=5.0').decode()
        e=dict(type='DECISION',id='ADA-4h-new',recorded_at=100,first_open=300,
               magnitude_time=live.new_contract(snapshot,100),
               original=dict(accepted=True,direction='BUY'),alternative=dict(accepted=False,direction='SELL'))
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            old=dict(type='DECISION',id='old')
            (root/'events-2026-10.jsonl').write_text(json.dumps(old)+'\n'+json.dumps(e)+'\n')
            receipt=dict(event_sha256=live.digest(e),confirmed_at=299)
            (root/'durability-receipts.json').write_text(json.dumps({e['id']:receipt}))
            self.assertEqual([(e,'original')],live.eligible(root))
            receipt['confirmed_at']=300
            (root/'durability-receipts.json').write_text(json.dumps({e['id']:receipt}))
            with self.assertRaises(ValueError):live.eligible(root)

    def test_no_registered_contracts_produces_empty_cohort_not_fabricated_results(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)/'root';work=Path(directory)/'work';data=Path(directory)/'data'
            root.mkdir();data.mkdir()
            (root/'durability-receipts.json').write_text('{}')
            (data/'manifest.json').write_text('{"inputs":{}}')
            live.prepare(root,data,work)
            self.assertEqual('',(work/'contracts.tsv').read_text())
            (work/'results.tsv').write_text('')
            live.finish(root,work)
            report=json.loads((root/'magnitude-summary.json').read_text())
            self.assertEqual(0,sum(c['signals'] for c in report['combinations']))
            self.assertFalse((root/'magnitude-outcomes.jsonl').exists())

    def test_invalid_range_rejected(self):
        snapshot=base64.b64encode(b'forecastLowPct=5.0, forecastHighPct=3.0').decode()
        with self.assertRaises(ValueError):live.new_contract(snapshot,100)
