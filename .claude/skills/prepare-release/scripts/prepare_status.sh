#!/usr/bin/env bash
# Prints the state of "[RM] Prepare release" for X.Y in a repo, one line:
#   absent            — no releases/X.Y and no active prepare run → dispatch
#   running <run-id>  — a prepare run is queued/waiting/in progress → wait for it, don't dispatch
#   done <run-id|->   — releases/X.Y exists and every job of the run that created it succeeded
#   partial <run-id>  — releases/X.Y exists but a job of that run failed (e.g. the Jira rename)
# Usage: prepare_status.sh <owner/repo> <X.Y>
set -euo pipefail

REPO="$1"; VERSION="$2"
WORKFLOW="prepare_release.yml"

active=$(gh run list -R "$REPO" --workflow "$WORKFLOW" --branch develop -L 20 --json databaseId,status \
    --jq '[.[] | select(.status != "completed")] | first | .databaseId // empty')
if [[ -n "$active" ]]; then
    echo "running $active"
    exit 0
fi

if ! gh api "repos/$REPO/branches/releases/$VERSION" --silent 2>/dev/null; then
    # Distinguish "no branch" from an API failure.
    gh api "repos/$REPO" --silent
    echo "absent"
    exit 0
fi

created=$(gh api "repos/$REPO/activity?ref=refs/heads/releases/$VERSION&activity_type=branch_creation" \
    --jq '[.[].timestamp] | min // empty')
run_id=""
if [[ -n "$created" ]]; then
    run_id=$(gh run list -R "$REPO" --workflow "$WORKFLOW" --branch develop -L 20 --json databaseId,createdAt \
        --jq "[.[] | select(.createdAt <= \"$created\")] | sort_by(.createdAt) | last | .databaseId // empty")
fi
if [[ -z "$run_id" ]]; then
    echo "done -"
    exit 0
fi

failed=$(gh run view "$run_id" -R "$REPO" --json jobs \
    --jq '[.jobs[] | select(.conclusion != "success" and .conclusion != "skipped")] | length')
if [[ "$failed" -gt 0 ]]; then
    echo "partial $run_id"
else
    echo "done $run_id"
fi
