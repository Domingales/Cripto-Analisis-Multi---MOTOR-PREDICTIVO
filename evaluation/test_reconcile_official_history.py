import unittest
from reconcile_official_history import choose_variant, reconcile_overlaps
from expanded_history import aggregate


class ReconciliationTests(unittest.TestCase):
    def setUp(self):
        self.parent={t:[t,10,12,9,11,100,t+299999] for t in range(0,900000,300000)}
        self.derived=aggregate(self.parent,900000)[0]

    def test_wrong_monthly_native_keeps_daily_corroborated_parent(self):
        native=[0,10,10,10,10,1,899999]
        policy,changes=choose_variant(self.parent,self.derived,native,self.parent,self.derived,900000)
        self.assertEqual(policy,'KEEP_CORROBORATED_DERIVED');self.assertEqual(changes,[])

    def test_monthly_parent_variant_requires_daily_native_and_rebuild(self):
        daily={k:list(v) for k,v in self.parent.items()};daily[0][2]=13;daily[0][5]=101
        native=aggregate(daily,900000)[0]
        policy,changes=choose_variant(self.parent,self.derived,native,daily,native,900000)
        self.assertEqual(policy,'USE_CORROBORATED_DAILY_5M_REBUILD_ALL_INTERVALS')
        self.assertEqual([x['open_time'] for x in changes],[0])
        self.assertEqual(self.parent[0][2],12)

    def test_third_variant_is_explicit_and_uncorroborated_daily_fails(self):
        native=[0,10,13,9,11,300,899999]
        with self.assertRaisesRegex(ValueError,'DAILY_5M_NATIVE_DISAGREE'):
            choose_variant(self.parent,self.derived,native,self.parent,native,900000)
        third=list(self.derived);third[2]=14
        daily={k:list(v) for k,v in self.parent.items()};daily[0][2]=14
        policy,changes=choose_variant(self.parent,self.derived,native,daily,third,900000)
        self.assertIn('THIRD_VARIANT',policy);self.assertEqual(len(changes),1)

    def test_missing_5m_is_never_filled_by_reconciliation(self):
        daily=dict(self.parent);del daily[300000]
        with self.assertRaisesRegex(ValueError,'MISSING_5M'):
            choose_variant(self.parent,self.derived,self.derived,daily,self.derived,900000)


    def test_higher_interval_correction_rechecks_previously_matching_lower_bucket(self):
        parent={t:[t,10,12,9,11,100,t+299999] for t in range(0,1800000,300000)}
        original_parent={t:list(v) for t,v in parent.items()}
        daily={t:list(v) for t,v in parent.items()}
        daily[600000][3]=8;daily[600000][4]=9;daily[600000][5]=101
        steps={'15m':900000,'30m':1800000}
        originals={tf:aggregate(original_parent,step) for tf,step in steps.items()}
        monthly={'15m':dict(originals['15m']),'30m':aggregate(daily,1800000)}
        def archive(tf,frequency,date):
            self.assertEqual(frequency,'daily')
            return daily if tf=='5m' else aggregate(daily,steps[tf])
        audit={'conflicts':[]}
        reconcile_overlaps(parent,original_parent,originals,monthly,archive,audit)
        self.assertEqual(parent,daily)
        self.assertEqual(set(parent),set(original_parent))
        proof=next(p for p in audit['conflicts'] if p['timeframe']=='15m')
        self.assertEqual(proof['stage'],'CROSS_INTERVAL_REBUILD')
        self.assertEqual(proof['original_derived'],monthly['15m'][0])
        self.assertEqual(proof['daily_native'],aggregate(parent,900000)[0])
        self.assertEqual(audit['reconciliation_passes'],2)


if __name__=='__main__':unittest.main()
