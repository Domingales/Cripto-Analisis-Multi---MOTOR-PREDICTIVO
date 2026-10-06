import base64
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
import selected_validation as validation
from prepare_selected_models import phase
import expanded_replay as replay


class SelectedValidationTests(unittest.TestCase):
    def test_selected_preparation_labels_end_before_new_observations(self):
        p=json.loads(validation.PROTOCOL.read_text())
        for tf in ('15m','4h','1d'):
            h=replay.horizon(tf)
            self.assertEqual(phase(p['alternative_train_end']-h-1,h,p),'TRAIN')
            self.assertEqual(phase(p['alternative_train_end']-h,h,p),'EXCLUDED')
            self.assertEqual(phase(p['alternative_calibration_label_end']-h,h,p),'EXCLUDED')
        self.assertEqual(len(p['selections']),15)
        self.assertEqual(len({tuple(x) for x in p['selections']}),15)

    def test_stale_future_or_backdated_records_are_rejected(self):
        t=1791331200000
        self.assertFalse(validation.is_fresh(t,t-1,t,'1h'))
        self.assertFalse(validation.is_fresh(t,t+2*replay.HOUR,t+2*replay.HOUR,'1h'))
        self.assertFalse(validation.is_fresh(t,t+1000,t+20*60000,'1h'))

    def test_forecasts_are_immutable_and_no_result_before_registered_horizon(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)/'ledger'; data=Path(d)/'data';data.mkdir()
            (data/'manifest.json').write_text(json.dumps({'inputs':{}}))
            now=1791331500000;close=now//3600000*3600000-1
            first=(now//300000+1)*300000;end=first+72*replay.HOUR-1
            snapshot=base64.b64encode(b'known context').decode()
            row=['ADA','4h',close,now,'true','BUY',100,1,first,end,close,snapshot]
            decisions=Path(d)/'decision.tsv';decisions.write_text('\t'.join(map(str,row))+'\n')
            validation.capture(root,data,decisions,Path(d)/'models','v1','engine',now=now)
            events=validation.load_events(root)
            self.assertEqual(len(events),1);self.assertEqual(events[0]['type'],'DECISION')
            validation.capture(root,data,decisions,Path(d)/'models','v1','engine',now=now+1000)
            self.assertEqual(validation.load_events(root),events)
            with self.assertRaises(ValueError):
                validation.capture(root,data,decisions,Path(d)/'models','v2','changed-engine',now=now+1000)

    def test_corrupt_frozen_model_is_rejected_before_loading(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);phash='protocol'
            manifest=dict(protocol_sha256=phash,status='FROZEN_FOR_NEW_PROSPECTIVE_VALIDATION',
                calibration_label_end=10,cutoff=20,examination_start=30,counts={},model_sha256='wrong')
            (p/'model_manifest.json').write_text(json.dumps(manifest))
            (p/'frozen_model.pkl').write_bytes(b'not a trusted model')
            with self.assertRaises(ValueError):validation.checked_model(p,phash)

    def test_result_window_starts_after_journal_write_not_buffered_computation(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)/'ledger';data=Path(d)/'data';data.mkdir()
            (data/'manifest.json').write_text(json.dumps({'inputs':{}}))
            computed=1791331500000; written=computed+600000
            close=computed//3600000*3600000-1
            first=(computed//300000+1)*300000;end=first+72*replay.HOUR-1
            row=['ADA','4h',close,computed,'true','BUY',100,1,first,end,close,base64.b64encode(b'context').decode()]
            decisions=Path(d)/'decision.tsv';decisions.write_text('\t'.join(map(str,row))+'\n')
            validation.capture(root,data,decisions,Path(d)/'models','v1','engine',now=written)
            e=validation.load_events(root)[0]
            self.assertEqual(e['computed_at'],computed)
            self.assertEqual(e['recorded_at'],written)
            self.assertGreater(e['first_open'],written)
            self.assertEqual(e['horizon_end']-e['first_open']+1,72*replay.HOUR)


if __name__=='__main__':unittest.main()
