import unittest
import numpy as np
from magnitude_candidate import phase,target,TRAIN_END,CAL_END,TEST_START,DAY
from expanded_replay import Features


class MagnitudeCandidateTest(unittest.TestCase):
    def test_purge_full_120h(self):
        for end in (TRAIN_END,CAL_END,TEST_START):
            self.assertEqual(phase(end-5*DAY),'EMBARGO')
        self.assertEqual(phase(TRAIN_END-5*DAY-1),'TRAIN')
        self.assertEqual(phase(TRAIN_END),'CALIBRATION')
        self.assertEqual(phase(CAL_END),'DEVELOPMENT_CHECK')
        self.assertEqual(phase(TEST_START),'EXAMINATION')

    def test_original_range_not_first_touch_target(self):
        self.assertEqual(target(3,7),5)
        for a,b in ((0,1),(2,1),(float('nan'),2),(1,float('inf'))):
            with self.assertRaises(ValueError):target(a,b)

    def test_future_mutation_does_not_change_features(self):
        a=np.array([[i*900000,100,102,99,100+i*.01,1,i*900000+899999] for i in range(250)],float)
        t=int(a[220,6]);x=Features({'x':a},{'x':900000}).at(t)
        a[221:,1:6]*=10
        self.assertEqual(x,Features({'x':a},{'x':900000}).at(t))
