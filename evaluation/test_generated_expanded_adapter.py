"""Keep the versioned test adapter identical to its production-preserving generator."""
import hashlib
import unittest
from prepare_expanded_adapter import ROOT, DOMAIN, generate


class GeneratedExpandedAdapterTests(unittest.TestCase):
    def test_versioned_adapter_matches_generator_and_production_stays_unchanged(self):
        target = ROOT / 'app/src/test/java/com/domingales/criptoanalisis/multi/domain/ExpandedResearchEngine.kt'
        original = (DOMAIN / 'BacktestEngine.kt').read_bytes()
        tracked = target.read_text()
        try:
            digest = generate()
            self.assertEqual(tracked, target.read_text(), 'Regenerate the versioned research adapter after changing its generator')
            self.assertEqual(digest, hashlib.sha256(original).hexdigest())
            self.assertEqual(original, (DOMAIN / 'BacktestEngine.kt').read_bytes())
        finally:
            target.write_text(tracked)
