import unittest
from prospective_ledger import resolve


class ResolutionTest(unittest.TestCase):
    def setUp(self):
        self.p = dict(first_open=300000, horizon_end=899999, entry=100.,
                      direction='BUY', target_pct=1.)
        self.c = [dict(openTime=str(t), closeTime=str(t + 299999), high='102', low='100')
                  for t in (300000, 600000)]

    def test_no_future_or_missing_window(self):
        self.assertIsNone(resolve(self.p, self.c, 899999))
        self.assertIsNone(resolve(self.p, self.c[1:], 900000))
        self.assertIsNone(resolve(self.p, self.c[::-1], 900000))

    def test_first_touch_and_ambiguity(self):
        self.c[1]['low'] = '98'
        self.assertEqual('HIT', resolve(self.p, self.c, 900000)['status'])
        self.c[0]['low'] = '98'
        self.assertEqual('NEUTRAL', resolve(self.p, self.c, 900000)['status'])
        self.c[0]['high'] = '100'
        self.assertEqual('FAIL', resolve(self.p, self.c, 900000)['status'])

    def test_sell(self):
        self.p['direction'] = 'SELL'
        self.assertEqual('FAIL', resolve(self.p, self.c, 900000)['status'])


if __name__ == '__main__':
    unittest.main()
