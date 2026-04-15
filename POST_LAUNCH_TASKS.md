# Post-Launch Task List — v1.0.3 → v1.1.0

> Generated 2026-04-15 after a full codebase walk. v1.0.3 is in Play Store first review; this is the punch list for what to fix before / for the next release. Grouped by area, ordered by user-visible impact.

---

## 1. Noise cleaning pipeline (highest priority — core product)

The on-device + server pipeline has working pieces but several gaps make output feel inconsistent.

- [ ] **Activate `ProcessingQualityGuard` in the live pipeline.** It exists (`audio/ProcessingQualityGuard.kt`) and is unit-tested, but is **never called** from `AudioProcessor.kt` or `MainActivity.kt`. Wire it into the file-clean post-processing path.
- [ ] **Implement retry → dry-mix → loudness-floor logic** that the tracker promises (Phase 5). Currently only the *detector* is built; the *response* is missing.
- [ ] **Fix silent JNI failures.** `RNNoise.kt:10–12` loads `librnnoise-lib.so` in a static block with no try/catch — distinguish "library not loaded" (fatal) from "state init failed" (recoverable) and surface a real error to the user.
- [ ] **Wrap all HTTP calls in `VoiceApiPhase2Client.kt` (lines 89–106 and siblings) in try/catch.** Today raw `IOException` / `SocketTimeoutException` propagates to `MainActivity` which only `printStackTrace()`s + shows a raw `.message` toast.
- [ ] **Add cloud → local fallback for Phase 2 flows.** `MainActivity.kt:1460–1597` (Reel / Add-background / Video) throws on server failure with no escape hatch. The legacy CLEAN flow has fallback (line 1212); modern flows don't.
- [ ] **Pre-flight 5 MB size check** before Phase 2 upload. Today the server returns 413 and we surface "HTTP 413". `FastLibTestScreen` already rejects at pick time — copy that pattern.
- [ ] **Polling backoff + sane timeout.** `MainActivity.kt:1504–1539` polls 120× at 1.5 s with no jitter, no exponential backoff, and the spinner stays up the full 3 minutes if the job is wedged.
- [ ] **Show recommended preset when overridden.** `MainActivity.kt:1417–1420` silently drops the adaptive recommendation if the user already moved off NORMAL — at least show a "Recommended: STRONG" hint.
- [ ] **Add try-finally around `audioProcessor.destroy()`** in the live record path so native state never leaks on crash.
- [ ] **Add a regression test for tail-padding.** Logic is correct in `AudioProcessor.kt:331–341` but uncovered.

## 2. Plan / billing screen (currently a Toast)

`PlanDialog` in `ui/screens/ProfileScreen.kt:630–824` renders, but the upgrade button at `MainActivity.kt:411–412` just shows `"Upgrade flow coming soon — ping support from Settings."` There is **no billing library** in `app/build.gradle.kts` or `gradle/libs.versions.toml`.

- [ ] **Decide payment provider.** Google Play Billing is the only Play-compliant option for a digital subscription on a Play-distributed app — RevenueCat / Stripe will get the listing rejected.
- [ ] **Add `com.android.billingclient:billing:7.x`** to `app/build.gradle.kts`.
- [ ] **Implement BillingClient lifecycle**: connect, query `SubscriptionsAvailable`, launch `launchBillingFlow`, handle `PurchasesUpdatedListener`.
- [ ] **Server endpoint to validate purchase tokens** and set `pro_expires_at` on `VoiceUser`. Wire into existing `/voice/auth/me/` (already returns `is_pro`, `pro_expires_at` — see `AuthApi.kt:31–44`).
- [ ] **Refresh `AuthUser` after successful purchase** so `isProMember` flips immediately.
- [ ] **Handle pending / deferred / refunded states** — Play Billing has these; don't only handle the happy path.
- [ ] **Test the full purchase flow** with a Play Console license-tester account before the next release.

## 3. UI / UX polish

The MD3 system was added (`MD3_DESIGN_SYSTEM.md`) but screens haven't been refactored to consume it.

### Hardcoded colors → MD3 tokens
- [ ] `LiveScreen.kt`: lines 230, 385, 548, 833, 852 — replace `Color(0xFFEF5350)` etc. with `MaterialTheme.colorScheme.error` / `tertiary`.
- [ ] `HomeScreen.kt`: lines 781, 786, 791, 1008, 1013, 1918, 1927, 1943, 2218, 2279.
- [ ] `ProfileScreen.kt`: lines 180, 224, 261, 268, 326, 751, 781 (gradients with hex literals).
- [ ] `ComparisonPlayerScreen.kt`: lines 30–36 import obsolete tokens (`AccentGreen`, `AccentCyan`, `BackgroundCard`); also lines 354, 362 use raw `Color.Black`.

### Missing states
- [ ] `HomeScreen` — empty state for "no recordings yet" with CTA to start recording.
- [ ] `HistoryScreen` — replace plain text empty state at line 196 with the existing `MD3EmptyState` component.
- [ ] `LiveScreen` — error UI for failed `AudioRecord.startRecording()`; offline banner.
- [ ] `ComparisonPlayerScreen` — surface MediaPlayer errors (currently silent at lines 64–77).

### Wrong / placeholder icons
- [ ] `LiveScreen.kt:215` — `Icons.Default.Menu` is being used as a microphone stand-in. Replace with proper Mic icon.
- [ ] `LiveRecordingCard` (lines 769–894) — Share / Download / Feedback buttons use semantically wrong icons.
- [ ] `FormatToggleChip` line 134 — emoji labels (🎙️ / 🎬) → real icon + text.

### Consistency / accessibility
- [ ] Standardise corner radii to MD3 tokens (4 / 8 / 12 / 16 dp). Currently a free-for-all across LiveScreen and ComparisonScreen.
- [ ] Add `contentDescription` to all icon buttons in `LiveScreen.kt:826–889`, `ComparisonPlayerScreen.kt:195–351`, `ProfileScreen.kt:140–161`.
- [ ] Audit touch targets — several compact icon buttons in the recording card are < 48 dp.
- [ ] Remove dead theme imports in `HomeScreen.kt:73–88`.
- [ ] De-duplicate `ComparisonScreen` and `ComparisonPlayerScreen` (~400 lines duplicated).

## 4. English-only strings (Play listing is India-EN today)

App is mostly English already. Real work is **string extraction**, not translation.

- [ ] `ui/VibeUi.kt:12, 31` — replace `"🌊 Ganga Calm"` with an English equivalent (e.g. `"Ocean Calm"`).
- [ ] **Move all hardcoded `Text("…")` strings into `res/values/strings.xml`.** Today only `app_name` exists there. ~200 strings to extract across ~18 Compose files. Without this, real localisation is impossible later.
- [ ] Toast and snackbar messages in `MainActivity.kt` — same treatment.
- [ ] Once extracted, add `values-hi/strings.xml` only if Hindi support is actually wanted (separate decision).

## 5. Process / hygiene

- [ ] `CLAUDE.md` cleanup: line 50 said `.env` was "checked in" — fixed in this branch. Spot-check the doc for other stale claims.
- [ ] Audit `.kilo/worktrees/placid-salesman/` — looks like an abandoned worktree mirroring the root. Either reactivate it or delete to reduce repo confusion.
- [ ] Decide what to do with the many `AUDIO_SAMPLE_CHECK_*.md` logs in the repo root — promote a "latest" one and archive the rest under `data/`.

---

## Suggested order for v1.1.0

1. Noise pipeline § 1 items 1–5 (the things that make the product feel broken).
2. Plan screen § 2 (revenue gate is currently a dead Toast — Play submission while this is non-functional risks "deceptive functionality" flag).
3. UI § 3 hardcoded colors + missing states.
4. String extraction § 4 (deferred — only matters before adding a second locale).
