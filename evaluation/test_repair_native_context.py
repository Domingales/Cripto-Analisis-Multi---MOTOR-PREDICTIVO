import unittest
from repair_native_context import merge_native


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


if __name__ == '__main__': unittest.main()
