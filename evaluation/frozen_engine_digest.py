"""Digest of the seven predictor files frozen before the live experiment.

Preserves the original sha256sum pipeline byte for byte. Additional independent
evaluators do not become predictor inputs; changing any listed file still fails.
"""
import hashlib
from pathlib import Path

NAMES = ('Assets', 'BacktestEngine', 'ExplosionDetector', 'Models',
         'OutcomeTracker', 'SignalEngine', 'TechnicalEngine')
DOMAIN = 'app/src/main/java/com/domingales/criptoanalisis/multi/domain/'


def engine_digest(root=Path('.')):
    listing = ''.join(hashlib.sha256((root / (DOMAIN + name + '.kt')).read_bytes()).hexdigest()
                      + '  ' + DOMAIN + name + '.kt\n' for name in NAMES)
    return hashlib.sha256(listing.encode()).hexdigest()


if __name__ == '__main__':
    print(engine_digest())
