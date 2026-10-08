import unittest
from xrp_episode_validation import episode_summary


def row(identifier, start, end):
    return dict(id=identifier, first_open=start, horizon_end=end)


class EpisodeTests(unittest.TestCase):
    def test_transitive_overlaps_select_earliest_even_when_it_fails(self):
        rows = [row('c', 18, 28), row('a', 0, 10), row('b', 9, 19), row('d', 29, 39)]
        s = episode_summary(rows, dict(a='FAIL', b='HIT', c='HIT', d='HIT'))
        self.assertEqual([x['signals'] for x in s['episodes']], [3, 1])
        self.assertEqual(s['counts']['FAIL'], 1)
        self.assertEqual(s['accuracy'], .5)

    def test_pending_first_cannot_be_replaced_by_later_hit(self):
        s = episode_summary([row('a', 0, 10), row('b', 5, 15)], {'b': 'HIT'})
        self.assertEqual(s['counts']['PENDING'], 1)
        self.assertIsNone(s['accuracy'])

    def test_boundary_and_duplicates(self):
        self.assertEqual(len(episode_summary([row('a', 0, 10), row('b', 10, 20)], {})['episodes']), 1)
        with self.assertRaises(ValueError):
            episode_summary([row('a', 0, 10), row('a', 20, 30)], {})
        with self.assertRaises(ValueError):
            episode_summary([row('a', 10, 0)], {})

    def test_neutral_and_empty_do_not_count_as_failure(self):
        self.assertIsNone(episode_summary([], {})['accuracy'])
        s = episode_summary([row('a', 0, 10)], {'a': 'NEUTRAL'})
        self.assertEqual(s['counts']['NEUTRAL'], 1)
        self.assertIsNone(s['accuracy'])
