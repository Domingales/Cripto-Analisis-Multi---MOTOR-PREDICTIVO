import copy
import unittest
import numpy as np
from volume_daily import DAY,STEP,signatures,historical_cost,strategies,metrics


class VolumeDailyTest(unittest.TestCase):
    def daily(self,n=70):
        return np.array([[i*DAY,101,102,99,100+i%2,2 if i%2 else 1,(i+1)*DAY-1]
                         for i in range(n)],float)

    def test_ratio_fifty_and_pp_ten(self):
        a=self.daily();flags,d=signatures(a,int(a[-1,6]))
        self.assertEqual(d['UD50']['ratio'],2)
        self.assertTrue(flags['UD50']);self.assertTrue(flags['PP10_VOLUME'])

    def test_pp_strict_and_does_not_use_up_day_max(self):
        a=self.daily();a[-3,5]=1000 # prior up day does not count as a down-volume maximum
        self.assertTrue(signatures(a,int(a[-1,6]))[0]['PP10_VOLUME'])
        a[-1,5]=1
        self.assertFalse(signatures(a,int(a[-1,6]))[0]['PP10_VOLUME'])

    def test_ratio_uses_previous_close_not_open_colour(self):
        a=self.daily();a[:,1]=101.5
        self.assertEqual(signatures(a,int(a[-1,6]))[1]['UD50']['ratio'],2)

    def test_zero_down_volume_is_not_infinite_confidence(self):
        a=self.daily();a[:,4]=100+np.arange(len(a))
        flags,d=signatures(a,int(a[-1,6]))
        self.assertIsNone(flags['UD50']);self.assertIsNone(flags['PP10_VOLUME'])
        self.assertIsNone(d['UD50']['ratio'])

    def test_gaps_and_short_context_not_evaluable(self):
        a=self.daily()
        for b in (a[-10:],np.delete(a,65,axis=0)):
            flags,_=signatures(b,int(a[-1,6]))
            self.assertIsNone(flags['UD50']);self.assertIsNone(flags['PP10_VOLUME'])

    def test_future_changes_cannot_change_signatures(self):
        a=self.daily(90);t=int(a[69,6]);expected=signatures(a,t)
        a[70:,1:6]*=100
        self.assertEqual(signatures(a,t),expected)

    def fixture(self):
        r=dict(id='XRP-1d-fixture',time=STEP-1,horizon_end=4*STEP-1,entry=100,target=1)
        a=np.array([[i*STEP,100,100.5,99.5,100,1,(i+1)*STEP-1] for i in (1,2,3)],float)
        return r,a

    def test_cost_missing_and_open_window_stay_pending(self):
        r,a=self.fixture()
        self.assertEqual(historical_cost(r,'BUY',a,cutoff=r['horizon_end'])['status'],'PENDING')
        self.assertEqual(historical_cost(r,'BUY',a[1:])['reason'],'MISSING_OFFICIAL_5M')

    def test_no_retrospective_profit_if_entry_outside_levels(self):
        r,a=self.fixture();a[0,1]=102
        self.assertEqual(historical_cost(r,'BUY',a)['status'],'NOT_EXECUTABLE')

    def test_stop_gap_and_ambiguous_bar(self):
        r,a=self.fixture();a[1,1:5]=[98,99,97,98]
        self.assertEqual(historical_cost(r,'BUY',a)['exit_prices'],[98])
        a[1,1:5]=[100,102,98,100]
        result=historical_cost(r,'BUY',a)
        self.assertEqual(result['status'],'AMBIGUOUS')
        self.assertLess(result['scenarios'][1]['return_low'],0)
        self.assertGreater(result['scenarios'][1]['return_high'],0)

    def test_costs_reduce_both_sides_and_target_gap_capped(self):
        r,a=self.fixture();a[1,1:5]=[102,103,101.5,102]
        result=historical_cost(r,'BUY',a)
        self.assertEqual(result['exit_prices'],[101])
        self.assertLess(result['scenarios'][2]['return_low'],result['scenarios'][0]['return_low'])
        self.assertEqual(historical_cost(r,'SELL',a)['reason'],'STOP_GAP')

    def test_zero_signals_is_not_success(self):
        self.assertIsNone(metrics([], {})['exact_rate'])

    def test_standalone_buy_does_not_relabel_original_sell(self):
        r=dict(original_direction='SELL',original_accepted=True,flags={'UD50':True,'PP10_VOLUME':False})
        s=strategies(r)
        self.assertEqual(s['UD50'],'BUY');self.assertEqual(s['ORIGINAL_ALL'],'SELL')
        self.assertNotIn('ORIGINAL_BUY_AND_UD50',s)
