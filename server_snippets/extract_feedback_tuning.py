"""
Recompute Demucs post-mix (vocals / original blend) from VoiceFeedback rows.

Singleton row [ExtractVoiceTuningState] pk=1 holds vocals_dry_ratio used by demucs_extract.
"""
from __future__ import annotations

import logging
import subprocess
import time
from pathlib import Path

from django.core.cache import cache

logger = logging.getLogger(__name__)

CACHE_KEY_LAST = "extract_voice_tuning_last"


def refresh_tuning_if_stale(max_seconds: int = 600) -> None:
    """At most one full recompute per window (per worker process cache)."""
    now = time.time()
    last = cache.get(CACHE_KEY_LAST)
    if last is not None and (now - float(last)) < max_seconds:
        return
    cache.set(CACHE_KEY_LAST, now, timeout=None)
    try:
        recompute_from_feedback()
    except Exception:
        logger.exception("extract_feedback_tuning: recompute failed")


def recompute_from_feedback() -> dict:
    from apps.voice.models import ExtractVoiceTuningState, VoiceFeedback

    qs = list(
        VoiceFeedback.objects.filter(mode_used__in=("extract_voice", "reel", "reel_mode")).order_by(
            "-created_at"
        )[:120]
    )
    state = ExtractVoiceTuningState.get_solo()
    if len(qs) < 4:
        summary = {"reason": "insufficient_feedback", "n": len(qs)}
        state.last_summary = summary
        state.save(update_fields=["last_summary", "updated_at"])
        return summary

    bad = 0.0
    good = 0.0
    for r in qs:
        w = 1.0 if r.mode_used == "extract_voice" else 0.4
        extra = r.extra_meta or {}
        notes = (r.notes or "").lower()
        issue = str(extra.get("issue_type") or "").lower()
        clear = extra.get("clear_voice") is True
        artifactish = (
            r.result_label in ("artifacts", "quiet")
            or "artifact" in issue
            or "noise" in issue
            or any(k in notes for k in ("robot", "hollow", "smooth", "metallic", "phase"))
        )
        if clear and r.result_label == "good":
            good += w
        elif artifactish:
            bad += w * (1.15 if ("smooth" in notes or "hollow" in notes or "robot" in notes) else 1.0)
        else:
            good += 0.35 * w

    denom = bad + good
    frac_bad = (bad / denom) if denom > 0 else 0.25

    # More complaints -> blend more original under vocals (warmer, fewer Demucs artifacts).
    dry = 0.03 + frac_bad * 0.26
    dry = max(0.02, min(0.30, dry))

    prev = float(state.vocals_dry_ratio)
    smoothed = prev * 0.35 + dry * 0.65
    smoothed = max(0.02, min(0.30, smoothed))

    state.vocals_dry_ratio = smoothed
    state.last_summary = {
        "n": len(qs),
        "bad_score": round(bad, 3),
        "good_score": round(good, 3),
        "frac_bad": round(frac_bad, 4),
        "raw_dry": round(dry, 4),
        "smoothed_dry": round(smoothed, 4),
        "prev_dry": round(prev, 4),
    }
    state.save()
    logger.info("extract_feedback_tuning: updated vocals_dry_ratio=%s", smoothed)
    return state.last_summary


def get_vocals_dry_ratio() -> float:
    from apps.voice.models import ExtractVoiceTuningState

    return float(ExtractVoiceTuningState.get_solo().vocals_dry_ratio)


def blend_vocals_dry(vocals: Path, original: Path, out: Path, dry_ratio: float) -> None:
    """Linear blend: (1-dry)*vocals + dry*original (mono, 48k PCM s16 output)."""
    dry = max(0.0, min(0.35, float(dry_ratio)))
    if dry < 0.002:
        subprocess.run(["cp", str(vocals), str(out)], check=True)
        return
    w_v = 1.0 - dry
    w_o = dry
    cmd = [
        "ffmpeg",
        "-y",
        "-i",
        str(vocals),
        "-i",
        str(original),
        "-filter_complex",
        (
            "[0:a]aresample=48000,aformat=sample_fmts=fltp,pan=mono|c0=c0[v0];"
            "[1:a]aresample=48000,aformat=sample_fmts=fltp,pan=mono|c0=c0[v1];"
            f"[v0][v1]amix=inputs=2:duration=first:dropout_transition=0:normalize=0:"
            f"weights={w_v} {w_o}[m]"
        ),
        "-map",
        "[m]",
        "-c:a",
        "pcm_s16le",
        str(out),
    ]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(f"ffmpeg blend failed: {r.stderr[-2500:]}")


def invalidate_tuning_cache() -> None:
    cache.delete(CACHE_KEY_LAST)
