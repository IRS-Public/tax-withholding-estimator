#!/bin/bash
# This script is useful if you want to see how your changes affect the built static assets
# You'll need to be on a new branch, as this script will compare:
# * The output of your current working copy
#   against
# * The output of the main branch in your local repository

set -euo pipefail

BASE_BRANCH="main"
WORKTREE_DIR="/tmp/$BASE_BRANCH-branch"
LOGS_PREFIX="[$(basename "$0")]:"

if [ "$(git branch --show-current)" = "$BASE_BRANCH" ]; then
  echo "$LOGS_PREFIX Already on main, nothing to compare."
  exit 0
fi

echo "$LOGS_PREFIX Checking out clean '$BASE_BRANCH' worktree..."
git worktree remove $WORKTREE_DIR 2>/dev/null || true
git worktree add $WORKTREE_DIR main

echo "$LOGS_PREFIX Building twe on \`$BASE_BRANCH\` worktree..."
(cd $WORKTREE_DIR && make twe)

echo "$LOGS_PREFIX Building twe on working copy..."
make twe

echo "$LOGS_PREFIX Diffing static assets..."
git diff --no-index --color=always $WORKTREE_DIR/out out | cat || true

echo "$LOGS_PREFIX Cleaning up..."
git worktree remove $WORKTREE_DIR
echo "$LOGS_PREFIX Diff complete."
