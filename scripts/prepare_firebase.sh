#!/usr/bin/env bash
# Prepare static site files for Firebase Hosting
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PUBLIC_DIR="$ROOT_DIR/firebase_public"

echo "Preparing firebase_public at $PUBLIC_DIR"
rm -rf "$PUBLIC_DIR"
mkdir -p "$PUBLIC_DIR"

# Copy all html and resource files from src/main/resources
SRC_RES="$ROOT_DIR/src/main/resources"
if [ -d "$SRC_RES" ]; then
  cp -R "$SRC_RES"/* "$PUBLIC_DIR"/
fi

echo "Done. firebase_public is ready."
