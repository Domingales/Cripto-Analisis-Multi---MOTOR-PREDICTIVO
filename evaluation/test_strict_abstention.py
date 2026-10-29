import unittest
from strict_abstention import phase, key, evidence, freeze, lower_bound, DAY, TRAIN_END, EXAM_START


def row(t,status='HIT'):
    return dict(time=t,direction='BUY',regime='RANGE',target=.5,status=status)


class StrictAbstentionTest(unittest.TestCase):
    def test_calendar_and_embargo(self):
        self.assertEqual(phase(EXAM_START),'EXAMINATION')
        self.assertEqual(phase(EXAM_START-5*DAY),'EMBARGO')
        self.assertEqual(phase(TRAIN_END-5*DAY),'EMBARGO')

    def test_target_bins_fixed(self):
        r=row(0)
        self.assertEqual(key(r,'regime_target'),'BUY|RANGE|LT1')
        r['target']=1
        self.assertEqual(key(r,'regime_target'),'BUY|RANGE|1_TO_2')
        r['target']=2
        self.assertEqual(key(r,'regime_target'),'BUY|RANGE|GE2')

    def test_zero_alerts_not_perfect(self):
        r=evidence([],3)
        self.assertIsNone(r['exact_rate'])
        self.assertFalse(r['strict_pass'])

    def test_one_success_not_certified(self):
        r=evidence([row(0)],3)
        self.assertEqual(r['exact_rate'],1)
        self.assertFalse(r['strict_pass'])
        self.assertLess(r['adjusted_wilson_lower'],.9)

    def test_pending_does_not_select_later_first(self):
        r=evidence([row(0,'PENDING'),row(DAY)],3)
        self.assertEqual(r['resolved'],1)
        self.assertEqual(r['first_episodes']['signals'],0)

    def test_future_outcomes_and_variables_not_used_in_freeze(self):
        rows=[row(TRAIN_END-20*DAY),row(TRAIN_END+DAY),row(EXAM_START)]
        labels={(r['time'],'BUY'):r['status'] for r in rows}
        a=freeze(rows,labels)
        rows[-1].update(regime='FUTURE_REGIME',target=100)
        labels[EXAM_START,'BUY']='FAIL'
        self.assertEqual(a,freeze(rows,labels))
        self.assertEqual(a['exam_labels_used'],0)

    def test_all_success_lower_not_one(self):
        self.assertLess(lower_bound(100,100,3),1)
        self.assertGreater(lower_bound(100,100,3),.9)
