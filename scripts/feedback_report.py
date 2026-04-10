#!/usr/bin/env python3
import argparse
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import urlopen


def fetch_feedback(url: str) -> list[dict]:
    with urlopen(url, timeout=20) as resp:
        payload = json.loads(resp.read().decode())
    return payload.get("items", [])


def summarize(items: list[dict]) -> dict:
    by_result = Counter(i.get("result_label", "unknown") for i in items)
    by_mode = Counter(i.get("mode_used", "unknown") for i in items)
    by_issue = Counter((i.get("extra_meta") or {}).get("issue_type", "").strip() or "none" for i in items)
    clear_yes = 0
    clear_no = 0
    for i in items:
        clear = (i.get("extra_meta") or {}).get("clear_voice")
        if clear is True:
            clear_yes += 1
        elif clear is False:
            clear_no += 1
    return {
        "total": len(items),
        "by_result": dict(by_result),
        "by_mode": dict(by_mode),
        "by_issue_type": dict(by_issue),
        "clear_voice_yes": clear_yes,
        "clear_voice_no": clear_no,
    }


def write_md(path: Path, summary: dict, items: list[dict]) -> None:
    now = datetime.now(timezone.utc).isoformat()
    recent = items[:10]
    lines = [
        "# Feedback Result Tracker",
        "",
        f"- Generated: `{now}`",
        f"- Total entries: `{summary['total']}`",
        f"- Clear voice yes/no: `{summary['clear_voice_yes']}/{summary['clear_voice_no']}`",
        "",
        "## Split by Result",
    ]
    for k, v in sorted(summary["by_result"].items(), key=lambda x: x[1], reverse=True):
        lines.append(f"- `{k}`: {v}")
    lines += ["", "## Split by Mode"]
    for k, v in sorted(summary["by_mode"].items(), key=lambda x: x[1], reverse=True):
        lines.append(f"- `{k}`: {v}")
    lines += ["", "## Split by Issue Type"]
    for k, v in sorted(summary["by_issue_type"].items(), key=lambda x: x[1], reverse=True):
        lines.append(f"- `{k}`: {v}")
    lines += ["", "## Recent Entries (Latest 10)", ""]
    for i in recent:
        em = i.get("extra_meta") or {}
        lines.append(
            f"- id={i.get('id')} ts={i.get('sample_timestamp')} mode={i.get('mode_used')} "
            f"result={i.get('result_label')} clear={em.get('clear_voice')} issue={em.get('issue_type', '')} "
            f"notes={i.get('notes', '')}"
        )
    lines += [
        "",
        "## Tuning Plan (Next)",
        "- Focus first on `extract_voice` artifacts and smoothness.",
        "- Add one mild post-smoothing profile for extract path and compare 5 takes.",
        "- Gate stronger denoise when signal is already clear to reduce robotic texture.",
        "- Track improvement target: artifacts ratio under 15% for extract mode.",
    ]
    path.write_text("\n".join(lines) + "\n")


def append_history(path: Path, summary: dict) -> None:
    record = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        **summary,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as f:
        f.write(json.dumps(record) + "\n")


def main() -> int:
    p = argparse.ArgumentParser(description="Fetch and summarize BolSaaf feedback data")
    p.add_argument("--url", default="https://shadowselfwork.com/voice/feedback/?limit=300")
    p.add_argument("--tracker-md", default="data/FEEDBACK_RESULT_TRACKER.md")
    p.add_argument("--history-jsonl", default="data/feedback_summary_history.jsonl")
    args = p.parse_args()

    items = fetch_feedback(args.url)
    summary = summarize(items)
    write_md(Path(args.tracker_md), summary, items)
    append_history(Path(args.history_jsonl), summary)
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
