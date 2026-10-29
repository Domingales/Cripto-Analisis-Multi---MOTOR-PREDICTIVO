import unittest
from confluence_replay import flags, choose, validate, spaced, COMBINATIONS, DAY


def evidence(p=.95):
    return dict(resolved=300,blocks14d=8,first_spaced=dict(signals=15),exact_rate=p,
                wilson95_first_spaced=.7)


class ConfluenceReplayTest(unittest.TestCase):
    def test_finite_search(self):
        self.assertEqual(len(COMBINATIONS),84)
        self.assertEqual(len(set(COMBINATIONS)),84)

    def test_only_closed_snapshot_flags(self):
        r=dict(x=[.01]*46,snapshot=dict(volume=2,adx=30,mtf=12,rsi=60,regime='TREND_UP',atr=1))
        before=flags(r,'BUY')
        self.assertTrue(all(before))
        r['status']='FAIL';r['future_price']=0
        self.assertEqual(before,flags(r,'BUY'))
        self.assertFalse(flags(r,'SELL')[2])

    def test_selection_uses_train_only(self):
        rules=[dict(id=str(i),train=evidence(),examination=dict(exact_rate=0)) for i in range(5)]
        a=[r['id'] for r in choose(rules,.8)]
        for r in rules:r['examination']['exact_rate']=1
        self.assertEqual(a,[r['id'] for r in choose(rules,.8)])
        self.assertEqual(len(a),3)

    def test_failure_validation_no_promotion(self):
        self.assertFalse(validate(evidence(.89),.8))
        self.assertTrue(validate(evidence(.95),.8))

    def test_no_support_not_perfect(self):
        r=evidence(1);r['resolved']=2
        self.assertFalse(validate(r,.8))

    def test_first_spaced_not_picked_by_success(self):
        rows=[dict(time=t*DAY,status=s) for t,s in [(0,'FAIL'),(1,'HIT'),(6,'FAIL')]]
        self.assertEqual([r['time']//DAY for r in spaced(rows)],[0,6])
