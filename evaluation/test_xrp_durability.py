import hashlib
import json
import unittest
from xrp_periodic_capture import verify_receipts


class DurabilityTests(unittest.TestCase):
    def event(self):
        return dict(type='DECISION',id='XRP-30m-1',recorded_at=100,first_open=300)

    def receipt(self,event,confirmed):
        return {event['id']:dict(event_sha256=hashlib.sha256(json.dumps(event,sort_keys=True).encode()).hexdigest(),confirmed_at=confirmed)}

    def test_remote_confirmation_must_precede_outcome_candle(self):
        event=self.event()
        verify_receipts([event],self.receipt(event,299))
        for timestamp in (99,300,301):
            with self.assertRaises(ValueError): verify_receipts([event],self.receipt(event,timestamp))

    def test_missing_receipt_and_modified_forecast_block_recovery(self):
        event=self.event()
        with self.assertRaises(ValueError): verify_receipts([event],{})
        receipt=self.receipt(event,150)
        event['recorded_at']=101
        with self.assertRaises(ValueError): verify_receipts([event],receipt)

    def test_outcomes_do_not_require_forecast_receipts(self):
        verify_receipts([dict(type='OUTCOME',id='one')],{})
