import hashlib
import json
import unittest
from selected_periodic_capture import verify


class SelectedDurabilityTests(unittest.TestCase):
    def test_receipt_must_match_and_precede_first_outcome_candle(self):
        event=dict(type='DECISION',id='ADA-4h-1',recorded_at=100,first_open=300)
        sha=hashlib.sha256(json.dumps(event,sort_keys=True).encode()).hexdigest()
        verify([event],{event['id']:dict(event_sha256=sha,confirmed_at=299)})
        for confirmed in (99,300):
            with self.assertRaises(ValueError):
                verify([event],{event['id']:dict(event_sha256=sha,confirmed_at=confirmed)})
