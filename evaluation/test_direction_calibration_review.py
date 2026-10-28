import json
import tempfile
import unittest
import ast
from pathlib import Path
from direction_calibration_review import metrics,target_bin,allowed_rows,joint_outcomes,uncertainty,DAY,TEST_START
from magnitude_candidate import read_labels


class DirectionCalibrationTest(unittest.TestCase):
    def items(self):
        return [dict(time=i*14*DAY,status='HIT' if i%2 else 'FAIL',probability=.5) for i in range(20)]

    def test_constant_fair_prediction_brier_and_auc(self):
        r=metrics(self.items(),.5)
        self.assertAlmostEqual(r['brier'],.25);self.assertAlmostEqual(r['auc'],.5)
        self.assertAlmostEqual(r['bias'],0);self.assertAlmostEqual(r['ece10_descriptive'],0)

    def test_pending_is_not_negative_and_zero_threshold_not_success(self):
        items=self.items()+[dict(time=21*14*DAY,status='PENDING',probability=.99)]
        r=metrics(items,.5)
        self.assertEqual(r['n'],20);self.assertEqual(r['pending'],1)
        self.assertIsNone(r['thresholds'][-1]['hit_rate'])

    def test_single_class_and_empty_auc_undefined(self):
        self.assertIsNone(metrics([],.5)['auc'])
        self.assertIsNone(metrics([dict(time=0,status='HIT',probability=.9)],.5)['auc'])

    def test_invalid_probability_rejected(self):
        for p in (-.1,1.1,float('nan')):
            with self.assertRaises(ValueError):metrics([dict(time=0,status='HIT',probability=p)],.5)

    def test_target_bins_fixed_boundaries(self):
        self.assertEqual(target_bin(.5),'[0.5,1)')
        self.assertEqual(target_bin(4),'[4,inf)')
        for q in (0,-1,float('inf')):
            with self.assertRaises(ValueError):target_bin(q)

    def test_future_variables_excluded_before_validation(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'features.jsonl'
            allowed=dict(time=1640995200000,phase='TRAIN',target=1,x=[.1]*45+[1])
            future=dict(time=TEST_START,phase='poison',target=-1,x=['FUTURE'])
            p.write_text(json.dumps(allowed)+'\n'+json.dumps(future)+'\n')
            baseline=allowed_rows(p)
            future.update(target=999,x=['CHANGED_FUTURE'])
            p.write_text(json.dumps(allowed)+'\n'+json.dumps(future)+'\n')
            self.assertEqual(allowed_rows(p),baseline)

    def test_phase_forgery_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'f.jsonl';p.write_text(json.dumps(dict(time=1640995200000,phase='CALIBRATION',target=1,x=[0]*45+[1])))
            with self.assertRaises(ValueError):allowed_rows(p)

    def test_both_direction_targets_can_hit(self):
        rows=[dict(time=0,phase='TRAIN')];labels={(0,s):dict(status='HIT') for s in ('BUY','SELL')}
        r=joint_outcomes(rows,labels,'TRAIN');self.assertEqual(r['both_exact'],1)

    def test_small_temporal_blocks_do_not_get_confidence_interval(self):
        r=metrics(self.items()[:3],.5,True)
        self.assertIsNone(r['uncertainty']['interval'])

    def test_bootstrap_reproducible(self):
        a=metrics(self.items(),.5,True);b=metrics(self.items(),.5,True)
        self.assertEqual(a['uncertainty'],b['uncertainty'])

    def test_examination_outcomes_not_validated_or_used(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'labels.tsv'
            before='1640995200000\tBUY\tHIT\tfixture\t1\t-1\n'
            p.write_text(before+f'{TEST_START}\tBUY\tPOISON_FUTURE\tinvalid\tbad\tbad\n')
            one=read_labels(p,before=TEST_START)
            p.write_text(before+f'{TEST_START}\tBUY\tOTHER_FUTURE\tinvalid\tbad\tbad\n')
            self.assertEqual(one,read_labels(p,before=TEST_START))
            self.assertEqual(len(one),1)

    def test_diagnostic_has_no_fit_calls(self):
        path=Path(__file__).with_name('direction_calibration_review.py')
        tree=ast.parse(path.read_text())
        forbidden=[n.func.attr for n in ast.walk(tree) if isinstance(n,ast.Call)
                   and isinstance(n.func,ast.Attribute) and n.func.attr in ('fit','partial_fit','fit_transform')]
        self.assertEqual(forbidden,[])
