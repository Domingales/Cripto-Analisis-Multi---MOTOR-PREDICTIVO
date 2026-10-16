#!/usr/bin/env bash
set -euo pipefail
git config user.name 'CriptoAnalisis evaluator'
git config user.email 'evaluator@users.noreply.github.com'
python3 evaluation/bootstrap_xrp_frozen.py
PYTHONPATH=evaluation python3 - <<'PY'
import json
from pathlib import Path
import download_long_history as history
from expanded_history import INTERVALS
from fetch_selected_data import collect
history.STEPS.update(INTERVALS)
collect(Path('xrp-current-data'),json.loads(Path('evaluation/xrp30m_validation_protocol.json').read_text()))
PY
python3 evaluation/prepare_expanded_adapter.py
SELECTED_PAIRS=XRP:30m \
SELECTED_DATA_DIR="$PWD/xrp-current-data" \
SELECTED_OUTPUT_DIR="$PWD/xrp-current-output" \
JAVA_TOOL_OPTIONS=-Xmx4g \
bash ./gradlew --no-daemon testDebugUnitTest --tests com.domingales.criptoanalisis.multi.evaluation.SelectedProspectiveTest
python3 evaluation/xrp_periodic_capture.py
python3 evaluation/execution_costs.py \
  --events evaluation/prospective-xrp/events-*.jsonl \
  --receipts evaluation/prospective-xrp/durability-receipts.json \
  --freeze-lock evaluation/prospective-xrp/freeze-lock.json \
  --data xrp-current-data --output evaluation/prospective-xrp/execution-costs.json
python3 -c "from sys import path; path.insert(0, 'evaluation'); from xrp_periodic_capture import persist; persist('Conservar simulación prospectiva de costes XRP30m')"

