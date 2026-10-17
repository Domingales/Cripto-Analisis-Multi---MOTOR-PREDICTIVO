import hashlib
import unittest
import tempfile
import zipfile
import json
from pathlib import Path
from unittest.mock import patch
import numpy as np
import audit_expanded_history as audit


class ExpandedAuditTest(unittest.TestCase):
    def test_gap_excludes_entire_bucket_and_tampering_fails(self):
        fine=np.array([[t, 10, 12, 8, 11, 2, t+299999] for t in range(0,1800000,300000)],dtype=float)
        expected=np.array([[0,10,12,8,11,6,899999],[900000,10,12,8,11,6,1799999]],dtype=float)
        audit.verify_aggregation(fine,expected,900000)
        audit.verify_aggregation(np.delete(fine,1,axis=0),expected[1:],900000)
        with self.assertRaises(ValueError):
            audit.verify_aggregation(np.delete(fine,1,axis=0),expected,900000)
        expected[0,5]+=1
        with self.assertRaises(ValueError):
            audit.verify_aggregation(fine,expected,900000)

    def test_corrupt_hash_and_coverage_rejected(self):
        blob=b'openTime,open,high,low,close,volume,closeTime\n0,10,12,8,11,2,299999\n'
        m=dict(csv_sha256=hashlib.sha256(blob).hexdigest(),count=1,start=0,end=600000,
               first=0,last=0,missing_ranges=[[300000,600000]],missing_candles=1)
        with patch.object(audit,'START',0), patch.object(audit,'END',600000):
            audit.validate(blob,m,'5m')
            with self.assertRaises(ValueError):
                audit.validate(blob+b'\n',m,'5m')
            with self.assertRaises(ValueError):
                audit.validate(blob,{**m,'missing_ranges':[]},'5m')

    def test_nonfinite_or_impossible_prices_rejected(self):
        for price in ('nan','7'):
            blob=('openTime,open,high,low,close,volume,closeTime\n0,10,'+price+',8,11,2,299999\n').encode()
            m=dict(csv_sha256=hashlib.sha256(blob).hexdigest(),count=1)
            with patch.object(audit,'START',0), patch.object(audit,'END',600000):
                with self.assertRaises(ValueError):
                    audit.validate(blob,m,'5m')

    def test_independent_official_prices_must_agree(self):
        start=1696291200000  # 2023-10-03 UTC
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/'sample.zip'
            with zipfile.ZipFile(path,'w') as z:
                z.writestr('coverage.json',json.dumps([dict(symbol='ADAUSDT')]))
                for tf,step in audit.INTERVALS.items():
                    z.writestr('ADAUSDT_'+tf+'.csv',
                        f'openTime,open,high,low,close,volume,closeTime\n{start},10,12,8,11,2,{start+step-1}\n')
            def official(symbol,tf,frequency,date,rows,manifest):
                rows[start]=[start,10,12,8,11,2,start+audit.INTERVALS[tf]-1]
                return True
            with patch.object(audit.history,'archive',side_effect=official):
                result=audit.official_parity(path)
                self.assertEqual(sum(x['status']=='MATCH_ON_ALL_DERIVED_CANDLES' for x in result),5)
            def changed(*args):
                official(*args)
                args[4][start][4]=10.5
                return True
            with patch.object(audit.history,'archive',side_effect=changed):
                with self.assertRaises(ValueError):
                    audit.official_parity(path)


if __name__ == '__main__':
    unittest.main()
