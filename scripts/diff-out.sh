#!/bin/bash
# This script is useful if you want to see how your changes affect the built static assets
# You'll need to be on a new branch, as this script will compare:
# * The output of your current working copy
#   against
# * The output of the main branch in your local repository

set -euo pipefail

BASE_BRANCH="main"
CURRENT_BRANCH="$(git branch --show-current)"
WORKTREE_DIR="/tmp/$BASE_BRANCH-branch"
LOGS_PREFIX="[$(basename "$0")]:"

cleanup() {
  echo "$LOGS_PREFIX Cleaning up..."
  git worktree remove $WORKTREE_DIR 2>/dev/null || true
  echo "$LOGS_PREFIX Done."
}

if [ "$CURRENT_BRANCH" = "$BASE_BRANCH" ]; then
  echo "$LOGS_PREFIX Already on \`$BASE_BRANCH\`, nothing to compare."
  exit 0
fi
echo "$LOGS_PREFIX Diffing \`$CURRENT_BRANCH\` against \`$BASE_BRANCH\`."

echo "$LOGS_PREFIX Checking out clean \`$BASE_BRANCH\` worktree..."
git worktree add $WORKTREE_DIR main
trap cleanup EXIT

echo "$LOGS_PREFIX Building twe on \`$BASE_BRANCH\` worktree..."
(cd $WORKTREE_DIR && make twe)

echo "$LOGS_PREFIX Building twe on working copy..."
make twe

echo "$LOGS_PREFIX Diffing build outputs..."
git diff --no-index --color=always $WORKTREE_DIR/out out | cat
if [ "${PIPESTATUS[0]}" -eq 0 ]; then
  echo "$LOGS_PREFIX No diff found. Build outputs on \`$CURRENT_BRANCH\` are equivalent with \`$BASE_BRANCH\`."
fi
