#!/usr/bin/env bash
# Scaffold a new project folder from the _template.
#
# Usage: ./scripts/new-project.sh my-new-tool

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Usage: $0 <project-name>"
  exit 1
fi

NAME="$1"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT_DIR/projects/_template"
DEST="$ROOT_DIR/projects/$NAME"

if [ -d "$DEST" ]; then
  echo "Error: projects/$NAME already exists."
  exit 1
fi

cp -r "$SRC" "$DEST"
sed -i.bak "s/\[tool name\]/$NAME/; s/\[Tool Name\]/$NAME/" "$DEST/CONTEXT.md" "$DEST/README.md"
rm -f "$DEST"/*.bak

echo "Created projects/$NAME/"
echo "Next: fill in projects/$NAME/CONTEXT.md, then point your AI at:"
echo "  - AGENTS.md (workspace root)"
echo "  - CONTEXT.md (workspace root)"
echo "  - projects/$NAME/CONTEXT.md"
