import unittest
import numpy as np
from repair_native_context import merge_native,carry_price_convention


class NativeRepairTests(unittest.TestCase):
    def test_official_bucket_restores_context_without_changing_fine_series(self):
        day = 86400000
        rows = {i*day: [i*day, 100, 102, 98, 101, 12, (i+1)*day-1]
                for i in range(330) if i != 280}
        original = dict(rows)
        t = 280*day
        fixed, added = merge_native(rows, {t:[t,100,102,98,101,12,t+day-1]}, day)
        self.assertEqual(added, [t])
        self.assertEqual(rows, original)
        self.assertEqual(len(fixed), 330)
        self.assertTrue(all(b-a == day for a,b in zip(sorted(fixed), sorted(fixed)[1:])))

    def test_conflicting_archive_cannot_silently_replace_prices(self):
        with self.assertRaises(ValueError):
            merge_native({0:[0,100,102,98,101,12,899999]},
                         {0:[0,100,103,98,101,12,899999]}, 900000)

    def test_nonstandard_close_is_rejected(self):
        with self.assertRaises(ValueError):
            merge_native({}, {0:[0,100,102,98,101,12,899998]}, 900000)

    def test_official_volume_variant_is_audited_without_overwriting_derived_bucket(self):
        original={0:[0,100,102,98,101,12,899999]}
        variants=[]
        result,added=merge_native(original,{0:[0,100,102,98,101,12.5,899999]},900000,variants)
        self.assertEqual(result,original)
        self.assertEqual(added,[])
        self.assertEqual(variants[0]['derived_volume'],12)
        self.assertEqual(variants[0]['native_volume'],12.5)

    def test_real_empty_candle_convention_is_proven_without_replacing_prices(self):
        t=1604258100000
        fine=np.array([[t,1.608,1.608,1.608,1.608,0,t+299999],
            [t+300000,1.6099,1.6106,1.6099,1.6106,3620.72,t+599999],
            [t+600000,1.6112,1.6197,1.6111,1.6197,1837.65,t+899999]])
        derived=[t,1.608,1.6197,1.608,1.6197,5458.37,t+899999]
        native=[t,1.6099,1.6197,1.6099,1.6197,5458.37,t+899999]
        checker=lambda t,old,new:carry_price_convention(fine,t,900000,old,new)
        proofs=[]
        result,added=merge_native({t:derived},{t:native},900000,price_checker=checker,price_conventions=proofs)
        self.assertEqual(result[t],derived);self.assertEqual(added,[])
        self.assertEqual(proofs[0]['zero_volume_open_times'],[t])
        bad=native.copy();bad[2]=1.7
        with self.assertRaises(ValueError):
            merge_native({t:derived},{t:bad},900000,price_checker=checker)

    def test_missing_fine_data_is_not_accepted_as_empty_candle_explanation(self):
        fine=np.array([[0,1,1,1,1,0,299999],[600000,2,2,2,2,1,899999]])
        self.assertIsNone(carry_price_convention(fine,0,900000,[0,1,2,1,2,1,899999],
            [0,2,2,2,2,1,899999]))


if __name__ == '__main__': unittest.main()
