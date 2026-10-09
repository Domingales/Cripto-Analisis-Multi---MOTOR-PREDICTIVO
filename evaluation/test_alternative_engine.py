import unittest
from alternative_engine import features, outcome


class AlternativeTests(unittest.TestCase):
    def test_features_ignore_future(self):
        candles=[dict(close=100+i,high=102+i,low=99+i,volume=20+i) for i in range(202)]
        original=features(candles,200)
        candles[201]['close']=99999
        self.assertEqual(original,features(candles,200))

    def test_intrabar_ambiguity(self):
        self.assertEqual(outcome([dict(high=102,low=98)],100,1,'BUY'),'NEUTRAL')

    def test_direction_and_first_touch(self):
        bars=[dict(high=100.5,low=98),dict(high=102,low=100)]
        self.assertEqual(outcome(bars,100,1,'BUY'),'FAIL')
        self.assertEqual(outcome(bars,100,1,'SELL'),'HIT')


if __name__=='__main__':
    unittest.main()
