import copy
import unittest
from unittest.mock import patch
import resume_expanded_replay as recovery

class ResumeReplayTests(unittest.TestCase):
    def summary(self):
        rows=[]
        for s in ('BTC','ADA'):
            for tf in recovery.INTERVALS:
                h=(336 if tf=='1d' else 72 if tf=='4h' else 24)*3600000
                counts={side:dict(train=100,calibration=100,latest_train_label=recovery.TRAIN_END-1,
                    latest_calibration_label=recovery.TEST_START-h-1) for side in ('BUY','SELL')}
                names={s+'USDT_'+k for k in ('5m','15m',tf,'4h','1d')}|{'BTCUSDT_4h'}
                f=dict(version=recovery.ENGINE_VERSION,threshold=.65,horizon_ms=h,
                    train_end=recovery.TRAIN_END,examination_start=recovery.TEST_START,
                    calibration_label_end=recovery.TEST_START-h,cutoff=recovery.CUT,
                    counts=counts,inputs={n:'b'*64 for n in names},
                    status='FROZEN_BEFORE_EXAMINATION',model_sha256='a'*64)
                m=dict(decisions=1,signals=0,HIT=0,FAIL=0,NEUTRAL=0,PENDING=0,resolved=0,accuracy=None)
                rows.append(dict(symbol=s,timeframe=tf,freeze=f,kotlin=dict(m),alternative=dict(m)))
        return dict(reports=rows,completed=10,expected=10,missing=[])

    def test_missing_interval_recomputes_entire_asset_and_preserves_other_reports(self):
        d=self.summary();d['reports']=[r for r in d['reports'] if not(r['symbol']=='ADA' and r['timeframe']=='15m')]
        d.update(completed=9,missing=[['ADA','15m']])
        with patch.object(recovery,'assets',return_value=['BTC','ADA']):
            reused,rerun=recovery.validate_prior(d)
        self.assertEqual(len(reused),5)
        self.assertEqual({s for s,tf in reused},{'BTC'})
        self.assertEqual(set(rerun),{('ADA',tf) for tf in recovery.INTERVALS})

    def test_mixed_engine_and_shared_data_versions_cannot_be_reused(self):
        d=self.summary();d['reports'][0]['freeze']['version']='unverified'
        with patch.object(recovery,'assets',return_value=['BTC','ADA']):
            with self.assertRaisesRegex(ValueError,'release'):recovery.validate_prior(d)
        d=self.summary();d['reports'][-1]['freeze']['inputs']['BTCUSDT_4h']='c'*64
        with patch.object(recovery,'assets',return_value=['BTC','ADA']):
            with self.assertRaisesRegex(ValueError,'input versions'):recovery.validate_prior(d)

    def test_future_training_label_cannot_be_reused(self):
        d=self.summary();d['reports'][0]['freeze']['counts']['BUY']['latest_train_label']=recovery.TRAIN_END
        with patch.object(recovery,'assets',return_value=['BTC','ADA']):
            with self.assertRaisesRegex(ValueError,'future label'):recovery.validate_prior(d)

if __name__=='__main__':unittest.main()
