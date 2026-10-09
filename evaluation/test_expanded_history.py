import unittest
from pathlib import Path
from expanded_history import aggregate, assets, INTERVALS

class ExpandedHistoryTest(unittest.TestCase):
    def test_catalog_and_intervals(self):
        symbols=assets(Path(__file__).resolve().parents[1])
        self.assertEqual(len(symbols),30)
        self.assertEqual(len(set(symbols)),30)
        self.assertEqual(set(INTERVALS),{'15m','30m','1h','4h','1d'})
    def test_exact_ohlcv(self):
        rows={i*300000:[i*300000,10+i,20+i,5+i,11+i,2,i*300000+299999] for i in range(3)}
        self.assertEqual(aggregate(rows,900000)[0],[0,10,22.0,5.0,13,6.0,899999])
    def test_gap_and_partial_buckets_excluded(self):
        rows={i*300000:[i*300000,1,2,.5,1,2,i*300000+299999] for i in (0,2,3,4)}
        self.assertEqual(aggregate(rows,900000),{})
    def test_future_bucket_not_completed(self):
        rows={i*300000:[i*300000,1,2,.5,1,2,i*300000+299999] for i in range(5)}
        self.assertEqual(list(aggregate(rows,900000)),[0])

if __name__=='__main__': unittest.main()
