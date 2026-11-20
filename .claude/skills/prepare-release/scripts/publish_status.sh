#!/usr/bin/env bash
# Prints the state of the release artifact for X.Y in an SDK repo, one line:
#   running <run-id>  — a publish run on releases/X.Y is queued/in progress → wait for it
#   done <version>    — the newest releases/X.Y-N tag points at the branch head; <version> is the
#                       artifact version for tangem_dependencies.toml (releases-X.Y-N)
#   absent            — no tag at the branch head (none yet, or the branch moved on) → publish
#   no-branch         — releases/X.Y doesn't exist yet (new release, before prepare)
# Usage: publish_status.sh <owner/repo> <X.Y>
# Any API failure exits non-zero, so it is never mistaken for "absent".
set -euo pipefail

REPO="$1"; VERSION="$2"
BRANCH="releases/$VERSION"

active=$(gh run list -R "$REPO" --workflow publish_artifact.yml --branch "$BRANCH" -L 20 --json databaseId,status \
    --jq '[.[] | select(.status != "completed")] | first | .databaseId // empty')
if [[ -n "$active" ]]; then
    echo "running $active"
    exit 0
fi

if ! head_sha=$(gh api "repos/$REPO/branches/$BRANCH" --jq '.commit.sha' 2>/dev/null); then
    # Distinguish "no branch" from an API failure.
    gh api "repos/$REPO" --silent
    echo "no-branch"
    exit 0
fi
tags=$(gh api "repos/$REPO/git/matching-refs/tags/$BRANCH-" --paginate --jq '.[] | "\(.ref) \(.object.sha)"')
latest=$(sed 's#^refs/tags/##' <<<"$tags" | { grep -E "^$BRANCH-[0-9]+ " || true; } | sort -V | tail -1)

if [[ -n "$latest" && "${latest#* }" == "$head_sha" ]]; then
    tag="${latest%% *}"
    echo "done ${tag//\//-}"
else
    echo "absent"
fi
