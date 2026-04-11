# Autoresearch Workflow (BolSaaf)

Use this only for analysis and experiment planning, not direct production patching.

## 1) Environment

From repo root:

```bash
cd tools/autoresearch
export PATH="$HOME/.local/bin:$PATH"
source .venv/bin/activate
set -a
source .env.local
set +a
```

## 2) Build Daily Brief

```bash
cd /home/vaibhav/AppWork2026/BolSaaf
python3 scripts/autoresearch_feedback_brief.py --root . --out data/autoresearch_daily_brief.md
```

This composes:
- `AUDIO_SAMPLE_CHECK_20260409_MATRIX.md`
- `data/feedback_summary_history.jsonl`
- `data/FEEDBACK_RESULT_TRACKER.md`
- `PHASE_PROGRESS_TRACKER.md`

## 3) Run Analysis With Agent

Open `tools/autoresearch` in agent mode and paste:

```text
Read ../data/autoresearch_daily_brief.md and produce:
1) Top 3 recurring issue clusters with frequency + severity
2) Mapping issue -> likely stage (denoise/compression/loudness/extract/bg mix)
3) Top 3 tuning experiments (param direction, risk, success metric for next 20 samples)
4) A small "do first tomorrow" checklist

Constraints:
- Do not modify production code automatically.
- Return recommendations only, with assumptions called out.
```

## 4) Human Review + Controlled Test

- Pick max 1-2 changes/day.
- Test on 4-5 fresh samples.
- Log results in feedback tracker.
- Only then patch server/app.

## 5) Quick Re-run

```bash
cd /home/vaibhav/AppWork2026/BolSaaf
python3 scripts/autoresearch_feedback_brief.py --root .
```
