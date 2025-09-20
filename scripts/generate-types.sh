#!/bin/bash
set -euo pipefail

TAG="1.0.8"
ASSET_NAME="openiap-kotlin.zip"
DOWNLOAD_URL="https://github.com/hyodotdev/openiap-gql/releases/download/${TAG}/${ASSET_NAME}"

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
TARGET_DIR="$REPO_ROOT/library/src/commonMain/kotlin/io/github/hyochan/kmpiap/openiap"
TARGET_FILE="$TARGET_DIR/Types.kt"

TEMP_DIR=$(mktemp -d)
ZIP_PATH="$TEMP_DIR/$ASSET_NAME"

cleanup() {
  rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

echo "⬇️  Downloading $ASSET_NAME from $DOWNLOAD_URL"
curl -fL "$DOWNLOAD_URL" -o "$ZIP_PATH"

echo "📦 Extracting Types.kt"
unzip -qo "$ZIP_PATH" Types.kt -d "$TEMP_DIR"
rm -f "$ZIP_PATH"

mkdir -p "$TARGET_DIR"
mv "$TEMP_DIR/Types.kt" "$TARGET_FILE"

# Ensure generated file declares the correct package
python3 - <<'PY' "$TARGET_FILE"
import sys

path = sys.argv[1]
package_line = "package io.github.hyochan.kmpiap.openiap\n"

with open(path, "r", encoding="utf-8") as f:
    lines = f.readlines()

if any(line.strip() == package_line.strip() for line in lines):
    sys.exit(0)

for index, line in enumerate(lines):
    if line.startswith("@file:Suppress"):
        insert_index = index + 1
        break
else:
    insert_index = 0

lines.insert(insert_index, "\n" + package_line + "\n")

with open(path, "w", encoding="utf-8") as f:
    f.writelines(lines)
PY

echo "✅ Types.kt has been updated at $TARGET_FILE"
