#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FDROID_REPO="${FDROID_REPO:-"$ROOT_DIR/../fdroiddata"}"
FDROID_REMOTE="${FDROID_REMOTE:-origin}"
FDROID_BRANCH="${FDROID_BRANCH:-master}"
WORKFLOW_FILE="${WORKFLOW_FILE:-android-release.yml}"

if ! command -v gh >/dev/null 2>&1; then
  echo "ERROR: gh (GitHub CLI) is required." >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: gh is not authenticated. Run 'gh auth login'." >&2
  exit 1
fi

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
RUN_ID="$(gh run list --workflow "$WORKFLOW_FILE" --branch "$TAG" --status success --limit 1 --json databaseId --jq '.[0].databaseId')"
if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
  echo "ERROR: No successful workflow run found for tag $TAG." >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

gh run download "$RUN_ID" -n fdroid-signatures -D "$TMP_DIR"

if [ -z "$(ls -A "$TMP_DIR")" ]; then
  echo "ERROR: No files found in downloaded fdroid-signatures artifact." >&2
  exit 1
fi

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

SIG_DEST="$FDROID_REPO/metadata/${APP_ID}/signatures/${VERSION_CODE}"
mkdir -p "$SIG_DEST"
cp -a "$TMP_DIR"/. "$SIG_DEST/"

python - <<PY
import re
from pathlib import Path

path = Path("$FDROID_REPO/metadata/${APP_ID}.yml")
text = path.read_text(encoding="utf-8")

def replace_once(pattern, replacement):
    new_text, count = re.subn(pattern, replacement, text, count=1, flags=re.M)
    if count != 1:
        raise SystemExit(f"Failed to update pattern: {pattern}")
    return new_text

text = replace_once(r'^(\\s*-\\s*versionName:\\s*).*$',
                    r'\\1${VERSION_NAME}')
text = replace_once(r'^(\\s*versionCode:\\s*).*$',
                    r'\\1${VERSION_CODE}')
text = replace_once(r'^(\\s*commit:\\s*).*$',
                    r'\\1${TAG}')
text = replace_once(r'^(CurrentVersion:\\s*).*$',
                    r'\\1${VERSION_NAME}')
text = replace_once(r'^(CurrentVersionCode:\\s*).*$',
                    r'\\1${VERSION_CODE}')

path.write_text(text, encoding="utf-8")
PY

git -C "$FDROID_REPO" add "metadata/${APP_ID}.yml"
git -C "$FDROID_REPO" -c core.autocrlf=false add "metadata/${APP_ID}/signatures/${VERSION_CODE}"
git -C "$FDROID_REPO" commit -m "bump ${APP_ID} to ${VERSION_NAME}"
git -C "$FDROID_REPO" push -u "$FDROID_REMOTE" "$BRANCH"

echo "fdroiddata updated on branch $BRANCH and pushed to $FDROID_REMOTE."
