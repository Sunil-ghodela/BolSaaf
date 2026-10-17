# ReelVoice — Design Polish Backlog (for Codex)

> Created 2026-10-17. This is the **"Android design polish"** workstream (product-plan item 3 in the Codex "Day work" plan, and the "UI / brand alignment" item in `HANDOFF_OPENAI_REELVOICE.md`).
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

1. **P0 nav-bar overlap fix** (one shared inset solution → fixes Live + Home).
2. **P0 shadow scale-down** (single pass across screens).
3. **P1 hero heading** + pluralisation + Live header inset.
4. **P2** polish pass.

Each item should be checked on a **real device** where possible (the emulator's rendering is close but not identical).

---

## Related docs

- `MD3_DESIGN_SYSTEM.md` — color/typography/elevation tokens.
- `HANDOFF_OPENAI_REELVOICE.md` — "UI / brand alignment" item.
- `POST_LAUNCH_TASKS.md` — broader post-launch punch list.
