#!/usr/bin/env bash
# 用法: ./tools/push-to-github.sh https://github.com/<user>/<repo>.git
set -euo pipefail

REPO_URL="${1:-}"
if [ -z "$REPO_URL" ]; then
  echo "用法: ./tools/push-to-github.sh <仓库地址>"
  exit 1
fi

if [ ! -d .git ]; then
  git init
fi

git add -A
if ! git diff --cached --quiet; then
  git commit -m "lio01 Android client v1.0.1"
fi

git branch -M main
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"
git push -u origin main
echo "完成: $REPO_URL"
