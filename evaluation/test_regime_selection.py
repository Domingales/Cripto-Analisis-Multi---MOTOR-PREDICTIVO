import csv, json, tempfile, unittest
from pathlib import Path
from unittest.mock import patch
from regime_selection import HOUR, RULE_HASH, classify, selection, live_variables, capture


def features(sign=1):
    x=[0.0]*36;x[16]=x[17]=x[34]=x[35]=sign*.02;x[6]=1.1
    return x


class RegimeTests(unittest.TestCase):
    def test_symmetric_trends(self):
        for sign,side,regime in ((1,'BUY','TREND_UP'),(-1,'SELL','TREND_DOWN')):
            self.assertEqual(classify(features(sign)),regime)
            self.assertTrue(selection(features(sign),side,True)[0])
            self.assertFalse(selection(features(sign),'SELL' if sign==1 else 'BUY',True)[0])
    def test_invalid_and_mixed_abstain(self):
        self.assertEqual(classify(None),'INVALID')
        x=features();x[16]=-0.1
        self.assertEqual(classify(x),'RANGE_OR_MIXED')
        self.assertFalse(selection(x,'BUY',True)[0])
        x[0]=float('nan');self.assertEqual(classify(x),'INVALID')
    def test_base_volume_btc_filters(self):
        self.assertFalse(selection(features(),'BUY',False)[0])
        x=features();x[6]=.99;self.assertEqual(selection(x,'BUY',True)[1],'INSUFFICIENT_VOLUME')
        x=features();x[34]=-.1;self.assertEqual(selection(x,'BUY',True)[1],'BTC_NOT_ALIGNED')
    def test_future_candles_do_not_change_context(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);t=300*24*HOUR-1
            for name,step in (('ADA_1h',HOUR),('ADA_4h',4*HOUR),('ADA_1d',24*HOUR),('BTC_4h',4*HOUR)):
                with (root/(name+'.csv')).open('w') as f:
                    w=csv.writer(f);w.writerow(['openTime','open','high','low','close','volume','closeTime'])
                    for j in range(300):
                        start=(t+1)-step*(300-j);price=1+j*.001
                        w.writerow([start,price,price+.1,price-.1,price,100,start+step-1])
            before=live_variables(root,t)
            self.assertEqual(len(before),36)
            with (root/'ADA_1h.csv').open('a') as f:
                f.write(f'{t+1},999,1000,998,999,99999,{t+HOUR}\n')
            self.assertEqual(live_variables(root,t),before)
            path=root/'ADA_4h.csv';lines=path.read_text().splitlines();del lines[-5];path.write_text('\n'.join(lines)+'\n')
            self.assertIsNone(live_variables(root,t))
    def test_no_backfill_or_late_capture(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);data=root/'data';data.mkdir()
            (data/'ADA_5m.csv').write_text('openTime,high,low,closeTime\n')
            base=dict(type='DECISION',id='x',recorded_at=1000,first_open=2000,accepted=False)
            (root/'events.jsonl').write_text(json.dumps(base)+'\n')
            with patch('regime_selection.time.time',return_value=3):
                capture(root,data,'v')
            self.assertFalse((root/'regime_shadow/events.jsonl').exists())
    def test_observation_is_durable_deduplicated_and_separate(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);data=root/'data';data.mkdir()
            (data/'manifest.json').write_text('{"sha256":{}}')
            (data/'ADA_5m.csv').write_text('openTime,high,low,closeTime\n')
            base=dict(type='DECISION',id='x',recorded_at=1000,first_open=300000,accepted=True,
                direction='BUY',candle_close=999,entry=1,target_pct=1,horizon_end=900000)
            original=json.dumps(base)+'\n';(root/'events.jsonl').write_text(original)
            with patch('regime_selection.time.time',return_value=2),patch('regime_selection.live_variables',return_value=features()):
                capture(root,data,'v');capture(root,data,'v')
            rows=[json.loads(s) for s in (root/'regime_shadow/events.jsonl').read_text().splitlines()]
            self.assertEqual(len(rows),1);self.assertEqual(rows[0]['rule_sha256'],RULE_HASH)
            self.assertEqual(rows[0]['mode'],'OBSERVATION_ONLY_REJECTED_FILTER')
            self.assertIsNone(rows[0]['probability'])
            self.assertEqual((root/'events.jsonl').read_text(),original)

if __name__=='__main__': unittest.main()
