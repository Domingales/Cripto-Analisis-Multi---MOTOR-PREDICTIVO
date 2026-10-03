import unittest
import numpy as np
import fixed_replay as replay


class ReplayTest(unittest.TestCase):
    def test_label_embargo(self):
        self.assertEqual(replay.partition(replay.TRAIN_END-24*replay.HOUR-1),'TRAIN')
        self.assertEqual(replay.partition(replay.TRAIN_END-24*replay.HOUR),'EMBARGO')
        self.assertEqual(replay.partition(replay.TRAIN_END),'CALIBRATION')
        self.assertEqual(replay.partition(replay.CAL_END-24*replay.HOUR),'EMBARGO')
        self.assertEqual(replay.partition(replay.TEST_START),'EXAMINATION')

    def fine(self):
        t=24*replay.HOUR-1
        a=np.array([[t+1+i*300000,100,100,100,100,10,t+(i+1)*300000] for i in range(288)],dtype=float)
        return t,a

    def test_both_touch_neutral_and_full_horizon_mfe(self):
        t,a=self.fine(); a[0,2]=102; a[0,3]=98; a[-1,2]=105
        r=replay.resolution(a,t,100,1,'BUY')
        self.assertEqual(r['status'],'NEUTRAL')
        self.assertEqual(r['reason'],'BOTH_TOUCHED_SAME_5M_CANDLE')
        self.assertEqual(r['mfe'],5)

    def test_missing_and_unexpired_are_pending(self):
        t,a=self.fine()
        self.assertEqual(replay.resolution(a[1:],t,100,1,'BUY')['reason'],'MISSING_5M_DATA')
        self.assertEqual(replay.resolution(a,t,100,1,'BUY',cutoff=t+24*replay.HOUR)['reason'],'WINDOW_NOT_EXPIRED')

    def test_first_touch_and_sell(self):
        t,a=self.fine();a[0,3]=98;a[1,2]=102
        self.assertEqual(replay.resolution(a,t,100,1,'BUY')['status'],'FAIL')
        self.assertEqual(replay.resolution(a,t,100,1,'SELL')['status'],'HIT')

    def test_context_closed_no_future_and_gap(self):
        a=np.array([[i*replay.HOUR,1,2,.5,1,2,(i+1)*replay.HOUR-1] for i in range(210)],dtype=float)
        t=int(a[199,6]);old=replay.closed_window(a,t,replay.HOUR).copy()
        a[200:,4]=999999
        np.testing.assert_array_equal(old,replay.closed_window(a,t,replay.HOUR))
        a[100,0]+=1
        self.assertIsNone(replay.closed_window(a,t,replay.HOUR))


if __name__=='__main__': unittest.main()
