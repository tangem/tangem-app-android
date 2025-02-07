#!/bin/bash

set -eo pipefail

# Input args
if [ -z "$1" ]; then
  echo "Error: No base branch argument provided or empty string given."
  echo "Usage: $0 <base-branch>"
  exit 1
fi
base_branch=$1

# Initialize arrays to store branch-commit mapping
declare -a branches
declare -a commits

# Source refs to find branches from
local_refs="refs/heads/releases/*"  # For debug and development
#TODO remove pre_release branches matching after 5.21 release
remote_refs=$(git for-each-ref --sort=-committerdate --format="%(refname:short)" refs/remotes/origin/ | grep -E "origin/([a-zA-Z0-9._-]+_pre_release|releases/[a-zA-Z0-9._-]+)$")

# Iterate over all remote release branches, sorted by commit date (most recent first)
index=0
for branch in $remote_refs; do
    # Find the latest commit that is an ancestor of base_branch using --first-parent strategy
    candidate_commit=$(git rev-list --first-parent "$branch..$base_branch" | tail -1)

    if [ -n "$candidate_commit" ]; then
        branches[$index]="$branch"
        commits[$index]="$candidate_commit"
        index=$((index + 1))
    fi
done

# Debug output of branches and commits
echo "Branches and their corresponding commits:" >&2
for i in "${!branches[@]}"; do
    echo "${branches[$i]} -> ${commits[$i]}" >&2
done

# Find the branch with the most recent commit
latest_branch=""
latest_commit=""
for i in "${!branches[@]}"; do
    branch=${branches[$i]}
    commit=${commits[$i]}
    if [ -z "$latest_commit" ] || [ "$(git rev-list --count $latest_commit..$commit)" -gt 0 ]; then
        latest_branch=$branch
        latest_commit=$commit
    fi
done

# Output validation
if [ -z "$latest_branch" ]; then
  echo "Error: Can't find the latest 'release/*' branch for the base branch '$base_branch'" >&2
  exit 2
fi

# Stripping 'origin/' prefix if needed
latest_branch="${latest_branch#origin/}"

echo "$latest_branch" > "find-latest-release-branch.output"
echo "Latest release branch created directly from '$base_branch' or its ancestor: '$latest_branch'"
