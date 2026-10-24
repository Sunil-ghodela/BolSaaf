# ReelVoice Android — next update execution plan

Updated: 23 October 2026. User scope: unfinished bug fixes and design, reliable background-sound addition, and forward/new-feature planning.

Baseline: Android 1.1.5 / versionCode 11 was previously verified live. Current working branch is `feature/live-recording-clarity`, HEAD `d923f33`. Existing uncommitted work must be preserved and reviewed. Do not bump version, upload, alter rollout, merge billing/video branches, or deploy server changes without the appropriate user direction.

## Current evidence (not completion claims)

- Pending local changes cover bounded WAV reading, recorder mic permission, larger-font preset tiles and camera permission result constants. New BoundedWavInput and its tests are untracked. The version 11/1.1.5 Gradle change is also uncommitted.
- Last fresh unit-test run in this conversation: 54 passed, 0 failures/errors/skips on 21 October. Must rerun after new work. Unit tests do not prove UI/audio quality.
- GET https://voice.tranquilwaters.in/voice/backgrounds/ returns nine backgrounds: rain, cafe, ocean, forest, street, bhakti, cricket, confession, study.
- Preview endpoint checks on 23 October: rain/cafe/ocean/forest/street return 404 on both HEAD and GET; bhakti/cricket/confession/study return HEAD 200 audio/mpeg. These do NOT establish mix-job success or audible quality. Audit outputs: build/background-catalog-audit.json and build/background-preview-audit.json.
- VoiceBackground already carries previewUrl/defaultVolume. No other Android source consumes previewUrl. HomeScreen VibePickerSheet currently chooses labels, without a preview player.
- MainActivity.refreshVoiceApiCapabilities() fabricates a Rain item on empty/error catalogue and nests background fetching behind health success. Selected background is tracked by list index, not stable ID.
- applyModePreset() can substitute a different background, including index 0 if no preferred/fallback ID exists. Existing IDs now exist on the server, but missing/error behavior remains a design/correctness gap.
- MainActivity privacy, terms and feedback still contain shadowselfwork.com URLs. That host failed DNS resolution during this audit; the active phase-2 client uses voice.tranquilwaters.in. Check the replacement routes before changing links.

## Work package 1 — reliable background-sound journey (first)

Target: choose sound -> hear sample -> choose voice clip/recording -> adjust mix -> process -> compare -> export/share -> find result in History.

1. **Catalogue truth:** fetch backgrounds independently of health; explicit loading/loaded-empty/error/retry states; no invented available item. Preserve selection by ID across refresh/reordering. A missing preset must not silently become rain/street/etc. Explain unavailable sounds and let the user choose.
2. **Usable picker:** show actual names, selection and preview as separate actions. Preview play/pause/loading/error state; one player at a time; stop/release on dismissal/navigation/lifecycle. Clear accessibility labels and large-font scrolling. A broken preview must not pretend to play; user can retry or select a supported alternative.
3. **Server preview repair:** inspect deployed assets and nginx/static paths for the five confirmed 404s. Prepare a scoped fix. Production asset/config changes require deployment direction; do not assume repository assets are deployed or licensed for redistribution.
4. **Correct mix:** snapshot selected ID + level when processing starts. Clearly distinguish background-only preview from the final voice+background result. Respect server min/max and source default volume; do not promise unsupported mute/custom upload behavior. Voice should remain intelligible; test clipping, track looping/end, voice/output duration and video sync.
5. **Success and recovery:** show upload/processing/download stages, readable errors, retry/cancel and quota/sign-in states. Do not silently fall back to a different effect or double-consume quota on retries.
6. **Output continuity:** original/processed A-B playback, save/share and persistent History metadata for sound ID/name and mix level where supported. Verify restart and removed-source behavior.

Acceptance: all advertised available sounds have working sample URLs; Android previews actually play and stop correctly; selected ID is the job ID; processed output audibly matches selection at tested levels; exported file is playable; no hidden substitution or infinite loading. Endpoint success alone is insufficient.

## Work package 2 — finish existing fixes and core regressions

- Review existing local WAV/permission changes, add missing boundary regression coverage, run JVM tests and debug build.
- Record/stop/background/foreground, original/cleaned playback, repeated recording, permission denial/revocation, offline use and process restart.
- File audio and video selection, cancellation, processing failure, download, export and History persistence.
- Verify legal/support/feedback URLs against the current host and migrate stale URLs without duplicating endpoint configuration.
- Keep billing/auth contracts unchanged. Smoke-test quota and entitlement states; actual purchases need licence-tester verification, not a UI-only check.

## Work package 3 — coherent design, not isolated polish

Home, Record/Live, background picker, comparison/result, History and Profile should share labels, spacing, tokens and state language. Audit small screen + enlarged font, bottom navigation clipping, touch targets, contrast and icon labels. Eliminate misleading placeholder copy and distinguish local vs cloud workflows. Do not repeat items marked completed in old backlogs without checking current UI.

Evidence: before/after screenshots and flow notes for changed screens, with branch/commit and known failures. Preserve existing user data; use disposable test recordings/accounts for QA.

## New-feature direction (proposals; not all promised for this update)

- **First candidate:** reusable last-used/favourite sound and mix setting, after stable-ID selection works. Helpful for repeat creation; small scope compared with a new editor.
- **Second candidate:** optional product-news/tips/offers opt-in, integrated with backend consent/unsubscribe. Do not force login for on-device recording or market to installer emails that we do not have.
- **Later:** user-imported background tracks, trimming/fades and voice ducking. Need an explicit media/rights/size/offline/server-contract design first; do not slip them into a polish release.

Default scope for this update is packages 1–3. Choose additional feature scope based on tested stability, not number of cards/features shipped. Marketing/web work stays linked to REELVOICE_GROWTH_PLAN_2026-10-23.md and does not block fixing current Android flows.

## Release gate

- Fresh unit tests and debug/release build checks as appropriate; no unreviewed local changes accidentally omitted from source control.
- Device/emulator evidence across core and background flows, including unavailable preview/catalogue and network/permission failures.
- Audible comparison on actual voice samples; emulator/build success is not sound-quality proof.
- No unsupported claims in UI/store copy; no personal media/credentials in artifacts.
- Final changed-files summary, commit, known gaps and release notes, then user-directed version bump/upload. No automatic release.

## Immediate next action

Implement and test catalogue/selection state and the picker preview flow, preserving existing edits. Investigate the missing server preview files read-only before preparing any deployment.
