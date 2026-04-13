#!/usr/bin/env python3
"""
Build a compact research brief for autoresearch from BolSaaf feedback files.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
from pathlib import Path


def read_text(path: Path, max_chars: int) -> str:
    if not path.exists():
        return f"[missing] {path}"
    text = path.read_text(errors="replace")
    if len(text) <= max_chars:
        return text
    return text[:max_chars] + "\n\n[truncated]"


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument(
        "--root",
        default=".",
        help="BolSaaf repo root",
    )
    p.add_argument(
        "--out",
        default="data/autoresearch_daily_brief.md",
        help="Output markdown path relative to root",
    )
    p.add_argument(
        "--max-chars-per-file",
        type=int,
        default=12000,
        help="Max characters to include from each source file",
    )
    args = p.parse_args()

    root = Path(args.root).resolve()
    out_path = (root / args.out).resolve()
    out_path.parent.mkdir(parents=True, exist_ok=True)

    sources = [
        root / "AUDIO_SAMPLE_CHECK_20260409_MATRIX.md",
        root / "data" / "feedback_summary_history.jsonl",
        root / "data" / "FEEDBACK_RESULT_TRACKER.md",
        root / "PHASE_PROGRESS_TRACKER.md",
    ]

    generated = datetime.now(timezone.utc).isoformat()

    parts = [
        "# BolSaaf Daily Tuning Brief",
        "",
        f"Generated at: `{generated}`",
        "",
        "## Task",
        "- Cluster recurring quality issues from feedback.",
        "- Map issues to likely pipeline stages (denoise/compression/loudness/extract/bg).",
        "- Suggest top 3 low-risk tuning experiments for next 20 samples.",
        "",
        "## Source Data",
    ]

    for src in sources:
        parts.append(f"- `{src.relative_to(root) if src.is_relative_to(root) else src}`")

    for src in sources:
        rel = src.relative_to(root) if src.is_relative_to(root) else src
        parts.extend(
            [
                "",
                f"## Content: `{rel}`",
                "```",
                read_text(src, args.max_chars_per_file),
                "```",
            ]
        )

    out_path.write_text("\n".join(parts))
    print(f"wrote {out_path}")


if __name__ == "__main__":
    main()
