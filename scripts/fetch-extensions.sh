#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS_DIR="$ROOT_DIR/app/src/main/assets/extensions"

mkdir -p "$ASSETS_DIR"

fetch_and_unpack() {
  local slug="$1"
  local dir="$2"
  local tmp
  tmp="$(mktemp -d)"
  echo "Fetching $slug..."
  curl -fsSL "https://addons.mozilla.org/firefox/downloads/latest/${slug}/latest.xpi" -o "$tmp/extension.xpi"
  rm -rf "$dir"
  mkdir -p "$dir"
  unzip -q "$tmp/extension.xpi" -d "$dir"
  rm -rf "$tmp"
  if command -v jq >/dev/null 2>&1; then
    local id
    id="$(jq -r '.browser_specific_settings.gecko.id // .applications.gecko.id // empty' "$dir/manifest.json")"
    echo "  -> $dir (id: ${id:-unknown})"
  else
    echo "  -> $dir"
  fi
}

fetch_and_unpack "p-stream-extension-v1" "$ASSETS_DIR/p-stream"
fetch_and_unpack "ublock-origin" "$ASSETS_DIR/ublock"
fetch_and_unpack "adguard-adblocker" "$ASSETS_DIR/adguard"

echo "Done. Extension assets unpacked to $ASSETS_DIR"
