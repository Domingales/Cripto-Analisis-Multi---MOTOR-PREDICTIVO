import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import numpy as np
import expanded_replay as replay
from fixed_replay import resolution as original_resolution


class ExpandedReplayTests(unittest.TestCase):
    def test_adapted_horizons_and_embargo(self):
        for tf,hours in [('15m',24),('30m',24),('1h',24),('4h',72),('1d',336)]:
            h=replay.horizon(tf); self.assertEqual(h,hours*replay.HOUR)
            self.assertEqual(replay.partition(replay.TRAIN_END-h-1,h),'TRAIN')
            self.assertEqual(replay.partition(replay.TRAIN_END-h,h),'EMBARGO')
            self.assertEqual(replay.partition(replay.TEST_START-2*h,h),'EMBARGO')
            self.assertEqual(replay.partition(replay.TEST_START-2*h-1,h),'CALIBRATION')
        self.assertEqual(replay.contexts('1h'),['15m','1h','4h','1d'])
        self.assertEqual(replay.contexts('1d'),['15m','1d','4h'])

    def test_outcomes_match_previous_resolution_in_24h(self):
        fine=np.array([[t,100,101,99,100,1,t+299999] for t in range(0,90000000,300000)],float)
        for side in ('BUY','SELL'):
            for target in (.5,1.5):
                self.assertEqual(replay.resolution(fine,299999,100,target,side,24*replay.HOUR),
                    original_resolution(fine,299999,100,target,side))
        self.assertEqual(replay.resolution(np.delete(fine,10,axis=0),299999,100,.5,'BUY',24*replay.HOUR)['reason'],'MISSING_5M_DATA')

    def test_daily_and_four_hour_windows_require_whole_horizon(self):
        for tf in ('4h','1d'):
            h=replay.horizon(tf)
            fine=np.array([[t,100,100.1,99.9,100,1,t+299999] for t in range(0,h+600000,300000)],float)
            self.assertEqual(replay.resolution(fine,299999,100,1,'BUY',h)['status'],'NEUTRAL')
            self.assertEqual(replay.resolution(fine[:-10],299999,100,1,'BUY',h)['reason'],'MISSING_5M_DATA')
            self.assertEqual(replay.resolution(fine,299999,100,1,'BUY',h,cutoff=h)['reason'],'WINDOW_NOT_EXPIRED')

    def test_future_data_cannot_change_features_and_context_gaps_reject(self):
        a=np.array([[t,100,101,99,100+t/100000000,1,t+899999] for t in range(0,300*900000,900000)],float)
        t=int(a[220,6]); baseline=replay.Features({'x':a},{'x':900000}).at(t)
        changed=a.copy(); changed[221:,1:6]*=10
        self.assertEqual(baseline,replay.Features({'x':changed},{'x':900000}).at(t))
        self.assertIsNone(replay.Features({'x':np.delete(a,100,axis=0)},{'x':900000}).at(t))
        stale=int(a[-1,6])+900000
        self.assertIsNone(replay.Features({'x':a},{'x':900000}).at(stale))

    def test_insufficient_preparation_is_not_zero_signal_success(self):
        with tempfile.TemporaryDirectory() as d:
            models,m=replay.freeze_model({'TRAIN':[],'CALIBRATION':[]},replay.horizon('1d'),Path(d),'test',{},[])
            self.assertEqual(models,[])
            self.assertTrue(m['status'].startswith('NOT_EVALUABLE'))
            self.assertIsNone(m['model_sha256'])
            self.assertFalse((Path(d)/'frozen_model.pkl').exists())

    def test_partial_aggregation_cannot_be_reported_complete(self):
        with tempfile.TemporaryDirectory() as d:
            with patch.object(replay,'assets',return_value=['ADA']):
                with self.assertRaises(ValueError): replay.aggregate_reports(Path(d),Path(d)/'out')
            m=json.loads((Path(d)/'out/summary_150.json').read_text())
            self.assertEqual(m['status'],'PARTIAL_NOT_COMPLETE'); self.assertEqual(len(m['missing']),5)


if __name__=='__main__': unittest.main()
