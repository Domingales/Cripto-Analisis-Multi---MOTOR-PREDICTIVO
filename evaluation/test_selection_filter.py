import unittest
from selection_filter import evaluate, HORIZON_MS


def row(t, outcome='HIT', direction='BUY'):
    return dict(symbol='ADA', timeframe='1h', direction=direction, confidence='85',
                signalCloseTime=str(t), outcome=outcome)


class SelectionTests(unittest.TestCase):
    def test_future_labels_do_not_change_prior_predictions(self):
        rows = [row(i*2*HORIZON_MS) for i in range(35)]
        _, original = evaluate(rows)
        rows[-1]['outcome'] = 'FAIL'
        _, changed = evaluate(rows)
        self.assertEqual([r['estimated_probability'] for r in original],
                         [r['estimated_probability'] for r in changed])
        self.assertTrue(original[-1]['accepted'])

    def test_overlapping_outcomes_not_used(self):
        _, predictions = evaluate([row(i*1000) for i in range(40)])
        self.assertTrue(all(r['comparable_cases']==0 for r in predictions))
        self.assertFalse(any(r['accepted'] for r in predictions))

    def test_direction_is_separate_and_neutrals_not_successes(self):
        rows = [row(i*2*HORIZON_MS, 'NEUTRAL') for i in range(31)]
        rows += [row(70*HORIZON_MS, direction='SELL')]
        _, predictions = evaluate(rows)
        self.assertEqual(predictions[-1]['comparable_cases'], 0)

    def test_duplicate_rejected(self):
        with self.assertRaises(ValueError):
            evaluate([row(0), row(0)])


if __name__ == '__main__':
    unittest.main()
