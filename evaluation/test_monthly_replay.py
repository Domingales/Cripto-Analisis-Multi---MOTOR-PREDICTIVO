import datetime as dt
import unittest
import monthly_replay as replay


class MonthlyTest(unittest.TestCase):
    def test_months_cover_exact_examination(self):
        periods=list(replay.months())
        self.assertEqual(len(periods),37)
        self.assertEqual(periods[0][0],replay.TEST_START)
        self.assertEqual(periods[-1][1],replay.CUT)
        self.assertTrue(all(a[1]==b[0] for a,b in zip(periods,periods[1:])))

    def test_labels_unknown_at_origin_never_enter_fit(self):
        origin=list(replay.months())[1][0]
        self.assertIsNone(replay.training_phase(origin,origin))
        self.assertIsNone(replay.training_phase(origin-48*replay.HOUR,origin))
        self.assertEqual(replay.training_phase(origin-48*replay.HOUR-1,origin),'CALIBRATION')

    def test_split_embargo_and_six_month_boundary(self):
        origin=list(replay.months())[1][0]
        start=replay.calibration_start(origin)
        self.assertEqual(dt.datetime.fromtimestamp(start/1000,dt.timezone.utc).isoformat(),'2023-05-01T00:00:00+00:00')
        self.assertEqual(replay.training_phase(start-24*replay.HOUR-1,origin),'TRAIN')
        self.assertIsNone(replay.training_phase(start-24*replay.HOUR,origin))
        self.assertEqual(replay.training_phase(start,origin),'CALIBRATION')

    def test_all_model_origins_have_no_future_labels(self):
        for origin,_ in replay.months():
            for lag in (0,1,12,24,36,48):
                self.assertIsNone(replay.training_phase(origin-lag*replay.HOUR,origin))


if __name__=='__main__':unittest.main()
