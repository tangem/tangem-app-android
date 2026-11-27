#!/usr/bin/env bash
# Checks that the previous release is fully merged back into develop in every release repo.
# Usage: preflight.sh <X.Y>
# Exit 0 — clean; exit 1 — blockers found (printed); exit 2 — usage/env error.
set -euo pipefail

VERSION="${1:-}"
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+$ ]]; then
    echo "Usage: preflight.sh <X.Y> (got '$VERSION')" >&2
    exit 2
fi

ORG="tangem-developments"
REPOS=(tangem-app-android tangem-sdk-android blockchain-sdk-kotlin)
BLOCKERS=0

for repo in "${REPOS[@]}"; do
    echo "== $repo"

    # Fetched separately so an API failure aborts the script instead of looking like "no branches".
    all_refs=$(gh api "repos/$ORG/$repo/git/matching-refs/heads/releases/" --paginate --jq '.[].ref')
    release_branches=$(sed 's#^refs/heads/##' <<<"$all_refs" \
        | { grep -E '^releases/[0-9]+\.[0-9]+(\.[0-9]+)?$' || true; } \
        | sort -V)

    if grep -qx "releases/$VERSION" <<<"$release_branches"; then
        echo "  INFO releases/$VERSION already exists (prepare already ran here)"
    fi

    last=$({ grep -vx "releases/$VERSION" <<<"$release_branches" || true; } | tail -1)
    if [[ -z "$last" ]]; then
        echo "  WARN no previous numeric release branch found"
    else
        ahead=$(gh api "repos/$ORG/$repo/compare/develop...$last" --jq '.ahead_by')
        if [[ "$ahead" -gt 0 ]]; then
            echo "  BLOCKER $last has $ahead commit(s) not merged into develop"
            BLOCKERS=$((BLOCKERS + 1))
        else
            echo "  OK $last is fully merged into develop"
        fi
    fi

    # The release being prepared opens its own sync-back PR once releases/$VERSION is pushed — not a blocker on resume.
    open_prs=$(gh pr list -R "$ORG/$repo" --state open --base develop --limit 200 \
        --json number,headRefName,title,url \
        --jq '.[]
            | select(.headRefName | test("^(merge/)?releases/"))
            | select(.headRefName != "releases/'"$VERSION"'" and .headRefName != "merge/releases/'"$VERSION"'_to_develop")
            | "  BLOCKER open PR #\(.number) \(.headRefName) -> develop: \(.url)"')
    if [[ -n "$open_prs" ]]; then
        echo "$open_prs"
        BLOCKERS=$((BLOCKERS + $(wc -l <<<"$open_prs")))
    else
        echo "  OK no open release->develop merge PRs"
    fi
done

if [[ "$BLOCKERS" -gt 0 ]]; then
    echo "RESULT: $BLOCKERS blocker(s)"
    exit 1
fi
echo "RESULT: clean"
