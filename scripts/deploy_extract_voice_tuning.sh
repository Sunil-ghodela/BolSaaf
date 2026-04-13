#!/usr/bin/env bash
# Deploy feedback-driven Demucs blend tuning to production Django host.
# Usage: ./scripts/deploy_extract_voice_tuning.sh [user@host]
set -euo pipefail
TARGET="${1:-root@77.237.234.45}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REMOTE_DIR="/var/www/simplelms/backend/apps/voice"

scp "$ROOT/server_snippets/extract_feedback_tuning.py" "$TARGET:$REMOTE_DIR/services/extract_feedback_tuning.py"
scp "$ROOT/server_snippets/demucs_extract_with_tuning.py" "$TARGET:$REMOTE_DIR/services/demucs_extract.py"

echo "Files copied. Run on server: append model, patch views, admin, migrate (see deploy_extract_voice_tuning_remote.py)."
