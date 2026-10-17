# ReelVoice — Design Polish Backlog (for Codex)

> Created 2026-10-17. This is the **"Android design polish"** workstream (product-plan item 3 in the Codex "Day work" plan, and the "UI / brand alignment" item in `HANDOFF_OPENAI_REELVOICE.md`).
>
> **Status (2026-10-17):** ALL P0 items are **DONE and verified on the emulator** — see "P0 — Status" at the end. **P1 items are also DONE and verified on the emulator** (API 35) — see "P1 — Status" at the end.
>
> Reference: `MD3_DESIGN_SYSTEM.md` (design tokens), Compose screens in `app/src/main/java/com/reelvoice/ui/screens/`.
> Screenshots reviewed: Home (Recent Cleans) and Live tab on emulator, 2026-10-17.

Priority: **P0** = obvious/visible bug, **P1** = polish, **P2** = nice-to-have.

---

## P0 — Card shadows too heavy

Owner feedback: "card pe shadow dekh, kitna hai" (Home "Recent Cleans" card shadow is very heavy).

Current elevation/shadow audit (verifiable):

| Location | Element | Elevation |
|---|---|---|
| `HomeScreen.kt:1563` | `HeroCleanPanel` (Record live card) | `shadowElevation = 6.dp` |
| `HomeScreen.kt:1812` | `SuccessCleanBanner` | `cardElevation = 8.dp` |
| `HomeScreen.kt:1921` | `ComparisonCard` (Recent Cleans row) | `6.dp` playing / `2.dp` idle |
| `HomeScreen.kt:2334` | section card | `shadowElevation = 8.dp` |
| `HomeScreen.kt:3269` | card | `cardElevation = 4.dp` |
| `HomeScreen.kt:1394` | elevated card | `2.dp` |
| `HomeScreen.kt:2589` | elevated card | `3.dp` |
| `HomeScreen.kt:1334` | surface | `shadowElevation = 2.dp` |
| `LiveScreen.kt:789` | stats card | `cardElevation = 6.dp` |
| `HistoryScreen.kt` / `ProfileScreen.kt` | cards | default elevation |

**Fix direction:** cap content cards at **1–2 dp** elevation (MD3 guidance). Keep a slightly higher value (3–4 dp) only for a genuinely raised element (active/playing card, dialog, bottom sheet). Remove the `6/8 dp` values. Prefer **`tonalElevation` + a subtle border** over drop shadows for separation.

**Verify:** Home (Recent Cleans + Record live), Live (stats card) — cards should look flat/soft, not "popped".

---

## P0 — Live screen: bottom element clipped behind the nav bar (the "red dot")

Owner feedback: "live vali screen pe neeche ye red dot".

**What it is:** the Live screen's bottom-most content (the large red record/play control in the **Recent Recordings** area) is centered at the very bottom and **overlaps / peeks out from behind `BottomNavBar`** — so only a red semicircle shows below the nav. Same symptom appears on Home (a red/coloured circle behind the nav).

**Root cause:** the screen content is not padded to clear the `BottomNavBar` height. The nav is drawn with `Modifier.align(Alignment.BottomCenter)` (`LiveScreen.kt:435`, `HomeScreen.kt:795`, etc.) over the content, and the last content element ends at (or below) the screen bottom.

**Fix direction:**
- Add **bottom content padding ≈ nav-bar height + system inset** to each scrollable/column body so the last item clears the nav (e.g. `WindowInsets.navigationBars` + a fixed nav height token).
- Or centralize: expose a single nav-bar height from `BottomNavBar` and apply it as `contentPadding`/spacer on all four screens.
- Confirm the same fix on Home (the "Record live" area bottom).

**Files:** `LiveScreen.kt` (bottom `Spacer(height = 40.dp)` at ~line 418 is insufficient), `HomeScreen.kt`, `HistoryScreen.kt`, `ProfileScreen.kt`, `ui/components/BottomNavBar.kt`.

**Verify:** Live last element fully visible above the nav; no coloured circle peeking below/behind the nav on any tab.

---

## P1 — Home hero heading doesn't read right

Owner feedback: "home pe title heading bhi shi nhi lag rahi".

**Current:** `HomeScreen.kt:1507` — `"Studio-grade voice"` at `displaySmall` (36sp) **Bold**, then `"Video + Audio"` at `headlineSmall` (24sp) with a mixed `primary` / `onSurfaceVariant` / `secondary` colour split.

**Fix direction:**
- Reduce the hero size (one step down, e.g. `headlineLarge`/`headlineMedium`) and rebalance the two lines' weight so the stack doesn't feel top-heavy.
- Tighten/standardise spacing between the two lines (currently an 8dp spacer).
- Simplify the colour treatment of the "Video + Audio" line (the `+` in `onSurfaceVariant` reads muddy) — e.g. consistent two-colour brand treatment or a single weight.

**Verify:** hero block in Home at top — balanced, on-brand, no oversized/awkward line break.

---

## P1 — Live "Recent Recordings" pluralisation

- Live shows **"1 files"**. Should be `"1 file"` (pluralise by count).

**Files:** `LiveScreen.kt` (Recent Recordings count label).

---

## P1 — Live header vs status bar

- In the reviewed Live screenshot the app header appears flush with the status bar, so the top-left red icon overlaps the system clock (`0:43`). Confirm the Live screen applies the correct **top `statusBars` inset** (Home looked fine).

**Files:** `LiveScreen.kt` top header.

---

## P2 — General polish (audit while in there)

- Standardise corner radii / spacing tokens across Home, Live, History, Profile (some cards are 16dp, 20dp, 24dp).
- Remove any remaining hardcoded `Color(0x...)` (the v1.1.0 pass replaced most — re-check new code).
- Consistent empty states (Home "No recordings yet" vs History filtered-empty) — same visual language.
- Dark theme pass: verify the reduced shadows + hero heading read well on the dark palette too.

---

## Suggested sequencing

1. ~~**P0 nav-bar overlap fix**~~ — DONE
2. ~~**P0 shadow scale-down**~~ — DONE
3. ~~**P1 hero heading** + pluralisation + Live header inset~~ — DONE (commit `4a1d8d5`)
4. **P2** polish pass

---

## P0 — Status (2026-10-17, commit `400afd8` on `integration/reelvoice-beta`)

1. **Shadows reduced** — every card/sheet at 6–10dp now sits at 1–4dp (Home `HeroCleanPanel`, `SuccessCleanBanner`, `ComparisonCard`, bottom sheets, Profile identity card `.shadow(10dp)`→`2dp`, Profile dialog `12dp`→`6dp`, Live stats card). Nav bar `shadowElevation` 16dp → 8dp.
2. **Nav-bar overlap fixed at the root** — the real cause was **forced edge-to-edge** (targetSdk 36 / Android 15) with no window-inset handling: content drew under both system bars. `MainActivity` now wraps the tab content in a `systemBars`-inset Box, so the header clears the status bar and the nav pill clears the system nav area. `LiveScreen` also got a `BottomNavBarBottomPadding` spacer so the 120dp record button is fully visible.
3. **Content peeking below the nav pill (the red dot) masked** — the floating pill's 10dp bottom margin let scrolled content (e.g. the red "Make Reel" banner) show through. `BottomNavBar` now draws a background gradient scrim behind itself.

Verified on emulator (API 35): Home + Live — no red dot, header inset correct, shadows visually softer. P1 (hero heading, "1 files", Live header) and P2 still open.

Each item should be checked on a **real device** where possible (the emulator's rendering is close but not identical).

---

## P1 — Status (2026-10-17, `integration/reelvoice-beta`)

Verified on emulator (API 35, 1080x2400, density 420). Evidence: `design-verify/` screenshots.

1. **Home hero heading rebalanced** — `"Studio-grade voice"` `displaySmall` (36sp) → `headlineLarge`; the `"Video + Audio"` line `headlineSmall` (24sp) → `titleLarge`. Stack no longer reads top-heavy; brand two-colour split kept.
2. **Live pluralisation fixed** — Recent Recordings count now shows `"1 file"` for a single item (was `"1 files"`). Verified after a live recording saved.
3. **Live header inset** — header clears the status bar on the emulator (no overlap with the system clock). No change needed.

### Additional fix shipped with the P1 pass

- **Home "Recent Cleans" card had a heavy grey frame (the shadow the owner flagged).** `ComparisonCard` used a `Card` with `containerColor = Transparent` + a 1–2dp elevation. The M3 tinted elevation shadow rendered as a hard grey backdrop/frame around the rounded card on device (the gradient only covered the inner content, so the tinted shadow showed as a frame). Set elevation to `0.dp` and rely on a hairline border + the existing gradient. Verified on emulator: the grey frame is gone (pixel check: card now transitions white → pink with no grey band).
- **Live record button was being clipped behind the nav bar (the "red dot") and/or overlapping the status text.** The WIP branch had pulled the record button out to a floating `BottomCenter` overlay, which overlapped `"Ready — tap record"` / `"Headphones recommended"`. Rebuilt the Live body as a **scrollable content region (`weight(1f)`) + a docked footer** (status + warning + record button) so the button is always fully visible and tappable above the nav, and no content overlaps it. Trimmed the mic visualizer (300dp → 240dp) and vertical rhythm so Home/Record/Live's common content fits without scrolling. Verified: idle, recording (`LIVE CLEANING`), and post-record states.

---

## Related docs

- `MD3_DESIGN_SYSTEM.md` — color/typography/elevation tokens.
- `HANDOFF_OPENAI_REELVOICE.md` — "UI / brand alignment" item.
- `POST_LAUNCH_TASKS.md` — broader post-launch punch list.
