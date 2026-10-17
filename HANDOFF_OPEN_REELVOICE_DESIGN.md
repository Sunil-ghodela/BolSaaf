# Open bhai handoff — ReelVoice design pass

Updated: 2026-10-17

## Goal

Polish the ReelVoice product surfaces while keeping the current audio/video direction and the existing backend contracts intact. This is a design/UI pass; do not mix it with the video-history persistence branch or billing implementation branch.

## Design scope

1. Remove the Lab/DEV surface wherever it is still visible.
2. Use the approved iOS logo/brand mark consistently in the app and landing-page brand areas.
3. Make audio and video actions read as one product: Quick Clean, Add Vibe, Video Clean, Reel/Video processing, and History should share hierarchy and state language.
4. Make loading, success, retry, offline, quota, and sign-in-required states visually explicit.
5. Keep Profile/Account ready for the real auth flow without making sign-in a gate for on-device recording.
6. Keep plan and entitlement labels aligned with `BILLING_CONTRACT.md`; do not hardcode new prices or limits in UI copy.
7. Preserve the landing page's public, lightweight feel. Ads and conversion tracking belong on public marketing sections, not inside private media workflows.

## Branch boundaries

- Design work: use a separate feature branch from the current base and report the commit hash.
- Video persistence: `feature/video-history-persistence` remains separate.
- Billing/product plan: `feature/reelvoice-product-plan` remains separate.
- Do not merge or rewrite another branch. Open a focused PR/commit for design changes.

## Deliverables

- Updated screen list with before/after screenshots or emulator notes.
- Changed files and a short rationale for each visual change.
- Accessibility labels for new icons/buttons and readable disabled/loading states.
- A smoke checklist covering Home, Record/Live, History, Profile, plan dialog, and video entry points.

## Definition of done

- No Lab/DEV tab remains in the user-facing build.
- Logo is consistent across launcher/app/profile/landing surfaces.
- Audio and video cards use consistent labels, spacing, icon treatment, and state colors.
- Design does not invent auth, billing, or quota behavior.
- Android build and the targeted emulator smoke flow pass.
- Handoff includes branch, commit, screenshots, known gaps, and merge notes.

## Notes for coordination

The Android app already has real auth and server entitlement plumbing. iOS account UI is still a placeholder, and the website currently has no login. Keep those implementation tasks for the account roadmap owner; this handoff should prepare the UI surfaces and states for them.
