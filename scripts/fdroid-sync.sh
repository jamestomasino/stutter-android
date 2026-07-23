#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FDROID_REPO="${FDROID_REPO:-"$ROOT_DIR/../fdroiddata"}"
FDROID_REMOTE="${FDROID_REMOTE:-origin}"
FDROID_BRANCH="${FDROID_BRANCH:-master}"

if [ ! -d "$FDROID_REPO/.git" ]; then
  echo "ERROR: fdroiddata repo not found at $FDROID_REPO" >&2
  exit 1
fi

read -r VERSION_NAME VERSION_CODE APP_ID < <(
  python - <<'PY'
import re
from pathlib import Path

text = Path("app/build.gradle.kts").read_text(encoding="utf-8")
app_id = re.search(r'applicationId\s*=\s*"([^"]+)"', text)
version_code = re.search(r'versionCode\s*=\s*(\d+)', text)
version_name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
if not app_id or not version_code or not version_name:
    raise SystemExit("Failed to parse applicationId/versionCode/versionName from app/build.gradle.kts")
print(version_name.group(1), version_code.group(1), app_id.group(1))
PY
)

TAG="v${VERSION_NAME}"

# Resolve tag to full commit hash (fdroiddata requires hashes, not tag refs)
COMMIT_HASH=$(git rev-parse "$TAG")

if [ -n "$(git -C "$FDROID_REPO" status --porcelain)" ]; then
  echo "ERROR: fdroiddata repo has uncommitted changes." >&2
  exit 1
fi

git -C "$FDROID_REPO" fetch "$FDROID_REMOTE" "$FDROID_BRANCH"
git -C "$FDROID_REPO" checkout "$FDROID_BRANCH"
git -C "$FDROID_REPO" pull --ff-only "$FDROID_REMOTE" "$FDROID_BRANCH"

BRANCH="stutter-${VERSION_NAME}"
if git -C "$FDROID_REPO" show-ref --verify --quiet "refs/heads/$BRANCH"; then
  echo "ERROR: Branch $BRANCH already exists in fdroiddata." >&2
  exit 1
fi
git -C "$FDROID_REPO" checkout -b "$BRANCH"

python - <<PY
import re
from pathlib import Path

path = Path("$FDROID_REPO/metadata/${APP_ID}.yml")
text = path.read_text(encoding="utf-8")

def replace_once(pattern, value):
    def repl(m):
        prefix = m.group(1)
        return prefix + value
    new_text, count = re.subn(pattern, repl, text, count=1, flags=re.M)
    if count != 1:
        raise SystemExit(f"Failed to update pattern: {pattern}")
    return new_text

text = replace_once(r'^(\s*-\s*versionName:\s*).*$',
                    '${VERSION_NAME}')
text = replace_once(r'^(\s*versionCode:\s*).*$',
                    '${VERSION_CODE}')
text = replace_once(r'^(\s*commit:\s*).*$',
                    '${COMMIT_HASH}')
text = replace_once(r'^(CurrentVersion:\s*).*$',
                    '${VERSION_NAME}')
text = replace_once(r'^(CurrentVersionCode:\s*).*$',
                    '${VERSION_CODE}')

path.write_text(text, encoding="utf-8")
PY

git -C "$FDROID_REPO" add "metadata/${APP_ID}.yml"
git -C "$FDROID_REPO" commit -m "bump ${APP_ID} to ${VERSION_NAME}"
git -C "$FDROID_REPO" push -u "$FDROID_REMOTE" "$BRANCH"

echo "fdroiddata metadata updated on branch $BRANCH and pushed to $FDROID_REMOTE."
