import unittest
import numpy as np
from movement_antecedents import forward_max, label, table, conditions, STEP


class AntecedentsTest(unittest.TestCase):
    def test_forward_extrema(self):
        np.testing.assert_equal(forward_max(np.array([1,4,2,3]),2),[4,4,3])

    def test_gap_not_negative(self):
        a=np.array([[i*STEP,100,105,99,100,10,(i+1)*STEP-1] for i in range(12)],float)
        hi=forward_max(a[:,2],12);lo=-forward_max(-a[:,3],12)
        self.assertTrue(label(a,(hi,lo),-1,100,4,1,'BUY'))
        self.assertIsNone(label(a[1:],(hi,lo),-1,100,4,1,'BUY'))

    def test_necessity_not_sufficiency(self):
        rows=[dict(time=i,entry=100,event=y,conditions={'a':a}) for i,(y,a) in enumerate([(True,True),(False,True)])]
        r=table(rows,'a',24)
        self.assertEqual(r['antecedent_coverage'],1)
        self.assertEqual(r['precision'],.5)

    def test_counterexample_refutes_always(self):
        r=table([dict(time=0,entry=100,event=True,conditions={'a':False})],'a',24)
        self.assertTrue(r['universal_refuted'])
        self.assertEqual(r['FN'],1)

    def test_unknown_not_absence(self):
        r=table([dict(time=0,entry=100,event=True,conditions={'a':None})],'a',24)
        self.assertEqual(r['unknown'],1)
        self.assertEqual(r['FN'],0)

    def test_future_snapshots_ignored(self):
        w=np.array([[i*1800000,100,102,98,100+i*.01,10,i*1800000+1799999] for i in range(200)],float)
        r=dict(time=int(w[-1,6]),x=[.01]*46)
        snap=dict(adx='30',mtfScore='10');snaps={r['time']-i*1800000:dict(adx='20') for i in (1,2,3)}
        a=conditions(w,r,snap,snaps,'BUY')
        snaps[r['time']+1800000]=dict(adx='1000')
        self.assertEqual(a,conditions(w,r,snap,snaps,'BUY'))
