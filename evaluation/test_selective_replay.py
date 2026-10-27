import copy
import json
import tempfile
import unittest
from pathlib import Path
from selective_replay import select, first_episodes, run


class SelectiveReplayTest(unittest.TestCase):
    def row(self, t=10, p=.91):
        return dict(time=t, horizon_end=t+10, symbol='XRP', timeframe='30m',
                    model_sha256='frozen', variables=[1],
                    alternative=dict(direction='SELL', probability=p, accepted=True),
                    kotlin=dict(direction='BUY', accepted=True))

    def test_threshold_and_no_context(self):
        self.assertTrue(select(self.row(p=.9)))
        self.assertFalse(select(self.row(p=.899)))
        self.assertFalse(select({**self.row(), 'variables': None}))
        self.assertFalse(select(self.row(p=None)))
        for p in (float('nan'), float('inf'), 1.1, -.1):
            with self.assertRaises(ValueError): select(self.row(p=p))

    def test_transitive_overlap(self):
        self.assertEqual(first_episodes([self.row(10), self.row(19), self.row(28), self.row(40)]), [10, 40])

    def test_labels_cannot_change_selection(self):
        r=self.row(); changed=copy.deepcopy(r)
        changed['result']={'status':'FAIL'}
        self.assertEqual(select(r), select(changed))

    def fixture(self, root):
        (root/'model_manifest.json').write_text(json.dumps(dict(status='FROZEN_BEFORE_EXAMINATION',
            model_sha256='frozen', train_end=0, calibration_label_end=1, examination_start=2)))
        (root/'decisions.jsonl').write_text(json.dumps(self.row())+'\n')
        (root/'outcomes.jsonl').write_text(json.dumps(dict(time=10,
            alternative={'status':'FAIL'}, kotlin={'status':'HIT'}))+'\n')

    def test_real_labels_and_no_certification(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d); self.fixture(root)
            report=run(root, root/'out')
            self.assertEqual(report['candidate']['FAIL'], 1)
            self.assertIsNone(report['certified_probability'])
            self.assertFalse(report['production_changed'])

    def test_missing_outcomes_preserves_prelabel_journal(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d); self.fixture(root); (root/'outcomes.jsonl').write_text('')
            with self.assertRaises(ValueError): run(root, root/'out')
            self.assertTrue((root/'out/selection-before-outcomes.jsonl').exists())

    def test_zero_signals_not_success(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d); self.fixture(root)
            (root/'decisions.jsonl').write_text(json.dumps(self.row(p=.5))+'\n')
            report=run(root, root/'out')
            self.assertEqual(report['candidate']['signals'], 0)
            self.assertIsNone(report['candidate']['resolved_accuracy'])

    def test_unordered_and_mixed_model_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d); self.fixture(root)
            (root/'decisions.jsonl').write_text('\n'.join(json.dumps(self.row(t)) for t in (20,10)))
            with self.assertRaises(ValueError): run(root, root/'out')
            (root/'decisions.jsonl').write_text(json.dumps({**self.row(), 'model_sha256':'other'}))
            with self.assertRaises(ValueError): run(root, root/'out')
