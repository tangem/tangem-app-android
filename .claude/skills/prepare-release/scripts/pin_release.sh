#!/usr/bin/env bash
# Phase 6 in one process: pins the release SDK builds and toggles on releases/X.Y through a PR.
# Usage: CO_AUTHOR="Claude <model>" pin_release.sh <X.Y> <card-version> <blockchain-version> <worktree-dir> [toggles...]
# PIN_DRY_RUN=1 stops after printing the diff (no commit, push or PR).
# Never touches the caller's checkout: all edits happen in <worktree-dir>. Prints the PR URL last.
# Resumable: reuses an existing PR, opens a PR for an already pushed branch, and cleans local-only
# leftovers of a failed run.
set -euo pipefail

VERSION="$1"; CARD="$2"; BLOCKCHAIN="$3"; WT="$4"; shift 4
: "${CO_AUTHOR:?set CO_AUTHOR, e.g. CO_AUTHOR=\"Claude Opus 5.5\"}"

GH_REPO="tangem-developments/tangem-app-android"
BASE="releases/$VERSION"
BRANCH="tech/${VERSION}_enable_toggles_sdk"
TITLE="Enable $VERSION feature toggles and set release SDK builds"
ROOT=$(git rev-parse --show-toplevel)
SET_TOGGLES="$ROOT/.claude/skills/prepare-release/scripts/set_toggles.py"

create_pr() {
    local body
    body=$(printf '## What\nPins Card SDK `%s` and Blockchain SDK `%s` for %s and sets the release feature toggles.\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n' \
        "$CARD" "$BLOCKCHAIN" "$VERSION")
    gh pr create -R "$GH_REPO" --base "$BASE" --head "$BRANCH" --label easy --title "$TITLE" --body "$body"
}

pr=$(gh pr list -R "$GH_REPO" --head "$BRANCH" --base "$BASE" --state all -L 1 --json url,state --jq '.[0] // empty | "\(.state) \(.url)"')
if [[ -n "$pr" ]]; then
    echo "PR already exists ($pr)" >&2
    echo "${pr#* }"
    exit 0
fi

if git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null; then
    ahead=$(gh api "repos/$GH_REPO/compare/$BASE...$BRANCH" --jq '.ahead_by')
    if [[ "$ahead" -eq 0 ]]; then
        echo "$BRANCH exists on origin with no commits over $BASE — delete it and rerun" >&2
        exit 1
    fi
    echo "$BRANCH already pushed without a PR — opening it" >&2
    create_pr
    exit 0
fi

git -C "$ROOT" fetch origin "$BASE"
git -C "$ROOT" worktree remove --force "$WT" 2>/dev/null || true
git -C "$ROOT" worktree prune

# A local branch is deleted only when it is a leftover of a failed run of this script: not checked
# out anywhere and holding nothing but this script's own commit on top of origin/$BASE.
if git -C "$ROOT" show-ref --verify --quiet "refs/heads/$BRANCH"; then
    foreign=$(git -C "$ROOT" log --format=%s "origin/$BASE..$BRANCH" | { grep -vxF "$TITLE" || true; })
    count=$(git -C "$ROOT" rev-list --count "origin/$BASE..$BRANCH")
    checked_out=$(git -C "$ROOT" worktree list --porcelain | { grep -xF "branch refs/heads/$BRANCH" || true; })
    if [[ -n "$foreign" || "$count" -gt 1 || -n "$checked_out" ]]; then
        echo "Local branch $BRANCH has other work or is checked out — not touching it. Remove or rename it and rerun." >&2
        exit 1
    fi
    git -C "$ROOT" branch -D "$BRANCH" >&2
fi

git -C "$ROOT" worktree add "$WT" -b "$BRANCH" "origin/$BASE"
trap 'git -C "$ROOT" worktree remove --force "$WT" 2>/dev/null || true' EXIT

cd "$WT"
perl -pi -e "s/^tangemCardSdk = \".*\"/tangemCardSdk = \"$CARD\"/; s/^tangemBlockchainSdk = \".*\"/tangemBlockchainSdk = \"$BLOCKCHAIN\"/" \
    gradle/tangem_dependencies.toml
python3 "$SET_TOGGLES" "$VERSION" "$@" >&2

unexpected=$(git diff --name-only | { grep -vE '^(gradle/tangem_dependencies\.toml|[^/]+/config-toggles/feature_toggles\.json)$' || true; })
if [[ -n "$unexpected" ]]; then
    echo "Unexpected changes, aborting: $unexpected" >&2
    exit 1
fi
if git diff --quiet; then
    echo "Nothing to change: $BASE already has these SDK builds and toggles" >&2
    exit 0
fi
git diff --stat >&2
if [[ "${PIN_DRY_RUN:-}" == "1" ]]; then
    git diff >&2
    exit 0
fi

git commit -q -a -F- <<EOF
$TITLE

Co-Authored-By: $CO_AUTHOR <noreply@anthropic.com>
EOF
git push -q -u origin "$BRANCH"
create_pr
