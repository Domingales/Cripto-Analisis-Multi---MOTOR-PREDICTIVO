#!/usr/bin/env bash
set -euo pipefail
git config user.name 'CriptoAnalisis evaluator'
git config user.email 'evaluator@users.noreply.github.com'
python3 evaluation/bootstrap_selected_frozen.py
python3 evaluation/fetch_selected_data.py --output selected-data
python3 evaluation/prepare_expanded_adapter.py
pairs=$(python3 -c "import json; p=json.load(open('evaluation/selected_validation_protocol.json')); print(','.join(s+':'+t for s,t in p['selections']))")
SELECTED_PAIRS="$pairs" \
SELECTED_DATA_DIR="$PWD/selected-data" \
SELECTED_OUTPUT_DIR="$PWD/selected-output" \
JAVA_TOOL_OPTIONS=-Xmx4g \
bash ./gradlew --no-daemon testDebugUnitTest --tests com.domingales.criptoanalisis.multi.evaluation.SelectedProspectiveTest
python3 evaluation/selected_periodic_capture.py
python3 evaluation/execution_costs.py \
  --events evaluation/selected-prospective/events-*.jsonl \
  --receipts evaluation/selected-prospective/durability-receipts.json \
  --freeze-lock evaluation/selected-prospective/freeze-lock.json \
  --data selected-data --output evaluation/selected-prospective/execution-costs.json
python3 -c "from sys import path; path.insert(0, 'evaluation'); from selected_periodic_capture import persist; persist('Conservar simulación prospectiva de costes de las 15 combinaciones')"

