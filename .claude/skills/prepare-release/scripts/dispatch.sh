#!/usr/bin/env bash
# Dispatches a workflow and prints the id of the run it created.
# Usage: dispatch.sh <owner/repo> <workflow-file> <ref> [key=value ...]
set -euo pipefail

REPO="$1"; WORKFLOW="$2"; REF="$3"; shift 3
FIELDS=()
for kv in "$@"; do FIELDS+=(-f "$kv"); done

# Never start a second run while one is queued, waiting for approval or in progress — reuse it.
active=$(gh run list -R "$REPO" --workflow "$WORKFLOW" --branch "$REF" -L 20 --json databaseId,status \
    --jq '[.[] | select(.status != "completed")] | first | .databaseId // empty')
if [[ -n "$active" ]]; then
    echo "Reusing active run $active of $WORKFLOW in $REPO on $REF" >&2
    echo "$active"
    exit 0
fi

me=$(gh api user --jq .login)
since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
if ! output=$(gh workflow run "$WORKFLOW" -R "$REPO" --ref "$REF" ${FIELDS[@]+"${FIELDS[@]}"} 2>&1); then
    echo "Dispatch of $WORKFLOW in $REPO on $REF failed: $output" >&2
    exit 1
fi

run_id=$(grep -oE '/actions/runs/[0-9]+' <<<"$output" | grep -oE '[0-9]+$' | head -1 || true)

# Older gh / API responses don't return the run URL — find our own run created after the dispatch.
attempt=0
while [[ -z "$run_id" && "$attempt" -lt 20 ]]; do
    sleep 3
    run_id=$(gh run list -R "$REPO" --workflow "$WORKFLOW" --branch "$REF" --event workflow_dispatch --user "$me" -L 5 \
        --json databaseId,createdAt \
        --jq "[.[] | select(.createdAt >= \"$since\")] | sort_by(.createdAt) | last | .databaseId // empty")
    attempt=$((attempt + 1))
done

if [[ -z "$run_id" ]]; then
    echo "Could not find the dispatched run of $WORKFLOW in $REPO. gh output: $output" >&2
    exit 1
fi
echo "$run_id"
