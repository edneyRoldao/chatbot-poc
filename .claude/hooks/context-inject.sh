#!/usr/bin/env bash
set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ti = d.get('tool_input', {})
    print(ti.get('file_path', ''))
except Exception:
    print('')
" 2>/dev/null || echo "")

[ -z "$FILE_PATH" ] && exit 0

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMMANDS="$PROJECT_ROOT/.claude/commands"

if echo "$FILE_PATH" | grep -qiE "(Repository\.java|/domain/.*\.java)"; then
    cat "$COMMANDS/jpa.md"
elif echo "$FILE_PATH" | grep -qiE "/messaging/.*\.java"; then
    cat "$COMMANDS/messaging-strategy.md"
elif echo "$FILE_PATH" | grep -qiE "/ai/.*\.java"; then
    cat "$COMMANDS/ai-integration.md"
elif echo "$FILE_PATH" | grep -qiE "/telegram/.*\.java"; then
    cat "$COMMANDS/telegram.md"
elif echo "$FILE_PATH" | grep -qiE "/(controller|service|mapper|dto|config)/.*\.java"; then
    cat "$COMMANDS/spring-arch.md"
fi

exit 0
