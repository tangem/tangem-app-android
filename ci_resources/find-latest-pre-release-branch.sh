#!/bin/bash

set -eo pipefail

# Input args
if [ -z "$1" ]; then
  echo "Error: No base branch argument provided or empty string given."
  echo "Usage: $0 <base-branch>"
  exit 1
fi
base_branch=$1

# Initialize variables to store the latest release branch and merge base
latest_branch=""
latest_merge_base=""

# Source refs to find branches from
local_refs="refs/heads/releases/*"  # For debug and development
remote_refs=$(git for-each-ref --format="%(refname:short)" refs/remotes/origin/ | grep -E "origin/[a-zA-Z0-9._-]+_pre_release$")

# Iterate over all remote release branches sorted by commit date (most recent first)
for branch in $remote_refs; do
    merge_base=$(git merge-base "$base_branch" "$branch")

    echo "Merge base for branches '$branch' and '$base_branch' is '$merge_base'"

    if [ -z "$latest_merge_base" ] || git rev-list "$merge_base..$base_branch" | grep -q .; then
        echo "Update"
        latest_branch="$branch"
        latest_merge_base="$merge_base"
    fi
done

echo "Latest branch: $latest_branch"
echo "Latest merge base: $latest_merge_base"


# Output validation
if [ -z "$latest_branch" ]; then
  echo "Error: Can't find the latest 'pre_release' branch for the base branch '$base_branch'"
  exit 2
fi

# Stripping 'origin/' prefix if needed
latest_branch="${latest_branch#origin/}"

echo "$latest_branch" > "find-latest-pre-release-branch.output"
echo "Latest release branch created directly from '$base_branch' or its ancestor: '$latest_branch'"
