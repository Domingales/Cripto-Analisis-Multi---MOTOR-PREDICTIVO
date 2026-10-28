import unittest
from six_year_original_audit import summarize, first_episodes, DAY


class SixYearAuditTest(unittest.TestCase):
    def test_pending_excluded_and_approximate_separate(self):
        r = summarize([dict(status=s) for s in ('HIT','APPROXIMATE','LATE','FAIL','PENDING')])
        self.assertEqual(r['resolved'], 4)
        self.assertEqual(r['exact_rate'], .25)
        self.assertEqual(r['exact_or_approximate_rate'], .5)

    def test_no_signals_not_zero_accuracy(self):
        self.assertIsNone(summarize([])['exact_rate'])

    def test_invalid_status_fails_closed(self):
        with self.assertRaises(ValueError):
            summarize([dict(status='UNKNOWN')])

    def test_transitive_selection_without_outcomes(self):
        rows = [dict(time=t*DAY, direction='BUY', status='FAIL') for t in (0,4,8,14)]
        self.assertEqual([r['time']//DAY for r in first_episodes(rows)], [0,14])
        for r in rows:
            r['status'] = 'HIT'
        self.assertEqual([r['time']//DAY for r in first_episodes(rows)], [0,14])

    def test_directions_separate(self):
        rows = [dict(time=0, direction=s) for s in ('BUY','SELL')]
        self.assertEqual(len(first_episodes(rows)), 2)
