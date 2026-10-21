#!/usr/bin/env bash
set -euo pipefail
criterion_root="$1"
criterion_data="$2"
criterion_work="$3"
python3 evaluation/magnitude_live.py prepare --root "$criterion_root" --data "$criterion_data" --work "$criterion_work"
MAGNITUDE_WORK="$PWD/$criterion_work" MAGNITUDE_DATA="$PWD/$criterion_data" \
bash ./gradlew --no-daemon testDebugUnitTest --tests com.domingales.criptoanalisis.multi.evaluation.MagnitudeLiveTest
python3 evaluation/magnitude_live.py finish --root "$criterion_root" --work "$criterion_work"
