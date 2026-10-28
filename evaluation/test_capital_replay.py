import unittest
from capital_replay import simulate, STEP


def bars(values):
    return {i*STEP: dict(open=p,close=p,closeTime=(i+1)*STEP-1) for i,p in enumerate(values)}


class CapitalReplayTest(unittest.TestCase):
    def test_cash_without_alerts(self):
        self.assertEqual(simulate([],bars([10,20]),start=0,end=2*STEP)['final_marked_value'],100)

    def test_delay_and_profit(self):
        s=[dict(time=STEP-1,direction='BUY'),dict(time=3*STEP-1,direction='SELL')]
        r=simulate(s,bars([1,1000,10,1000,20]),start=0,end=5*STEP,fee=0,slip=0)
        self.assertEqual(r['trades'][0]['execution_time'],2*STEP)
        self.assertEqual(r['final_marked_value'],125)

    def test_costs_and_open_position_valued(self):
        r=simulate([dict(time=STEP-1,direction='BUY')],bars([10]*4),start=0,end=4*STEP)
        self.assertGreater(r['open_units'],0)
        self.assertLess(r['final_marked_value'],100)
        self.assertLess(r['hypothetical_net_liquidation'],r['final_marked_value'])

    def test_missing_execution_cancelled(self):
        b=bars([10]*4);del b[2*STEP]
        r=simulate([dict(time=STEP-1,direction='BUY')],b,start=0,end=4*STEP)
        self.assertEqual(r['buys'],0)
        self.assertEqual(r['skipped'][0]['reason'],'NO_EXACT_EXECUTION_BAR')

    def test_never_sell_loss_is_separate(self):
        s=[dict(time=STEP-1,direction='BUY'),dict(time=3*STEP-1,direction='SELL')]
        b=bars([10,10,10,5,5])
        self.assertEqual(simulate(s,b,start=0,end=5*STEP,no_loss=True)['sells'],0)
        self.assertEqual(simulate(s,b,start=0,end=5*STEP)['sells'],1)

    def test_future_prices_cannot_change_past_trades(self):
        s=[dict(time=STEP-1,direction='BUY')]
        a=simulate(s,bars([10,10,10,15]),start=0,end=4*STEP)
        b=simulate(s,bars([10,10,10,1]),start=0,end=4*STEP)
        self.assertEqual(a['trades'],b['trades'])

    def test_duplicate_signals_rejected(self):
        with self.assertRaises(ValueError):
            simulate([dict(time=1,direction='BUY')]*2,bars([10]),start=0,end=STEP)
