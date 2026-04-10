# BolSaaf Next 7-Day Plan

Last updated: 2026-04-10
Owner: Team BolSaaf

## Weekly Goal

- Reduce `extract_voice` artifacts and improve smoothness.
- Stabilize reel backend chain end-to-end.
- Keep feedback-driven iteration loop active daily.

## Success Targets (End of Week)

- Extract mode feedback: `artifacts <= 15%`
- At least `35` real feedback entries collected
- Reel pipeline smoke pass rate: `>= 95%` for tested cases

## Day-wise Execution

### Day 1-2: Extract Tuning Sprint

- [ ] Run 5 real takes/day (`quiet`, `normal`, `noisy`, mixed speech)
- [ ] Collect app feedback from testers (Yes/No + issue type)
- [ ] Refresh tracker: `python3 scripts/feedback_report.py`
- [ ] Apply only 1 controlled tuning tweak/day
- [ ] Record notes in benchmark log

### Day 3: Guard Calibration

- [ ] Tune dry-mix trigger sensitivity for `extract_voice`
- [ ] Tune loudness floor thresholds for very quiet outputs
- [ ] Compare before/after on same sample set
- [ ] Keep changes minimal and reversible

### Day 4-5: Reel Backend Hardening

- [ ] Validate chain: clean/extract -> bg mix -> loudness -> export
- [ ] Verify timeout/retry behavior on long files
- [ ] Confirm status polling + final output URL consistency
- [ ] Run smoke for reel and video endpoints

### Day 6: UX Trust Layer

- [ ] Add stage-wise processing labels:
  - `Analyzing -> Cleaning -> Mixing -> Finalizing`
- [ ] Keep feedback dialog fast and low-friction
- [ ] Confirm no regressions in Home/History/Live cards

### Day 7: Freeze + Tester Push

- [ ] Build release APK
- [ ] Push to friend tester batch
- [ ] Collect final day feedback
- [ ] Publish weekly tracker snapshot

## Daily Routine (Must Follow)

- [ ] Test 5-10 samples
- [ ] Run `python3 scripts/feedback_report.py`
- [ ] Review:
  - `data/FEEDBACK_RESULT_TRACKER.md`
  - `data/feedback_summary_history.jsonl`
- [ ] Pick next micro-fix from top issue only

## Risk Watch

- If artifacts increase after tuning, rollback last tweak immediately.
- If feedback volume is low, prioritize tester outreach over code changes.
- If reel chain fails repeatedly, pause UX work and fix backend stability first.
