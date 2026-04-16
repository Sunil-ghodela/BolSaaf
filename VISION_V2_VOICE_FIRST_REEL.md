# BolSaaf v2 — Voice-First Reel Studio for Indian Youth

> Date: 2026-04-16. Successor to `PHASE_PROGRESS_TRACKER.md`. Owner: Sunil / Vaibhav.
> Status: strategic plan, not yet green-lit. Read first, discuss, then pick phases.

---

## 1. Why change direction

### Market reality (April 2026)

- **India is the #1 Reels market on Earth** — 413 M users, ~2.5× the US. Post-TikTok ban, Reels + Shorts own the attention economy for 18-30 y/o.
- **Creator economy in India**: $15.03 B in 2026 → $61.87 B by 2033 (22.4 % CAGR).
- **Monetization gap**: Instagram pays ~₹60 per 1,000 views (bonus programs). YouTube Shorts pays ₹0.50–3. IG is 10–100× better for Indian creators *today*.
- **AI video tool demand up 300 %** since 2024. Hindi + regional captions are now table stakes, not a differentiator.
- **IG native affiliate** went live March 2026 in India. YT dropped affiliate threshold from 1,000 subs to 500. Creators want to monetize *within* the editor flow.

### Competitor read

| App | Strength | Weakness we can exploit |
|---|---|---|
| CapCut | Unlimited exports, trending Hindi effects, AI voice clean | Visual-first; audio is an afterthought; "clean studio" voice only — fails on chawl / balcony / chai-shop recordings |
| InShot | Simplest, 5+ Indian language auto-captions, ₹199/mo | Generic; no voice specialization |
| VN | Watermark-free, beat-sync, pro feel | No AI features; tiny India distribution |
| Descript | Voice-centric desktop editor, "delete word from transcript = delete from video" | Desktop-only, English-first, ₹1,500+/mo |
| VEED / InVideo AI | 99 % accuracy for Dravidian languages | Web-first, laggy on sub-₹15 k phones |
| TrueFan AI | AI avatars for voice-only creators | Studio-grade voice required (we can supply it) |

**The gap nobody owns**: a **voice-first reel creator for noisy Indian environments**. Every competitor assumes a quiet room and a visual timeline. Indian reality is a scooter horn outside, a fan on the ceiling, mummy yelling from the kitchen.

Our existing pipeline (on-device RNNoise + server Demucs + `ProcessingQualityGuard` dry-mix) is **2–3 years ahead** of CapCut's single-pass denoiser on Indian noisy-env input. That is a real moat — if we lean into it.

## 2. Repositioning

From: *"Android voice cleaner app"*
To: **"Bolo. App reel bana degi."** — a voice-first reel studio where you *speak*, the app *cleans + styles + publishes*.

Principle: **do not fight CapCut on visual editing**. They have 100s of devs; we're one. Fight them on the one axis they are weak at: the voice *as the primary medium*. Video is a byproduct.

Target user profile:
- 17–28, tier-2/3 India, one-handed phone user.
- Aspiring content creator OR small-biz owner doing product demos OR student doing study vlogs / exam tips.
- Doesn't want a timeline. Wants to *talk to the phone* and get a reel out.
- Budget ₹100–250 / month for a tool that gets them 10× the views.

## 3. Phased roadmap

Phase numbers continue from the existing tracker.

### Phase 3 — Voice-First Reel MVP (v1.2, ~4–6 weeks)

Ship the "speak → reel" core loop.

- **Hindi + 6 regional language auto-captions** on the cleaned track (higher accuracy than competitors because we caption *post-clean*, not *pre-clean*). Languages: Hindi, Marathi, Tamil, Telugu, Bengali, Gujarati, Punjabi.
- **"Best-take" picker** — user records 3–5 attempts, server scores them (clarity, emotion, pace), app auto-selects + lets user override.
- **Desi background pack** — replace generic "cafe / rain / forest" with:
  - Chai shop ambience
  - Mumbai local train
  - Temple aarti / bells
  - Monsoon on tin roof
  - Cricket stadium cheer
  - Study room with ceiling fan
  - Late-night Delhi street
- **Animated waveform video** — for voice-only posts (huge on IG right now): upload pic + record voice → app generates a reel with waveform animated to voice + captions.
- **One-tap share** to Instagram Reels, YouTube Shorts, WhatsApp Status.
- **Onboarding**: 30-sec flow — pick vibe, record 15 s, see magic. No sign-up gate.

Success metric for Phase 3: 60 % of first-time users ship at least one reel in the first session.

### Phase 4 — Voice AI Studio (v1.3, ~6–8 weeks)

Unlock the "wow" features that competitors cannot copy in 6 months.

- **Voice effects library** — Bollywood echo, news anchor, dubbed voice, anime, deep, helium, robot, chipmunk. Server-side.
- **Auto-dub across languages** — record in Hindi, generate English / Marathi / Tamil version *in your own voice* (ElevenLabs-grade voice clone, billed per minute). Huge for regional creators targeting pan-India.
- **AI Ghostwriter** — type "roast WhatsApp forwards" → app generates a 30-sec script + reads it *in your voice* + cuts to reel length. Creator doesn't have to face-cam if anxious.
- **Filler-word remover** — auto-delete "umm", "aaa", "matlab", "bhai woh", "actually". Descript-style, but tuned for Hinglish.
- **Awkward-pause trimmer** — silence > 0.5 s auto-trimmed. Tightens the reel without manual cutting.
- **Speaker separation** — for 2-person interviews, auto-detect speakers, caption each separately.

Success metric: 30 % of active users use at least one Phase-4 feature per week.

### Phase 5 — Creator Growth Tools (v1.4, ~8–10 weeks)

Move from "cool tool" to "must-have for growth".

- **Trending audio library** — curated weekly, copyright-cleared, tagged by mood / pace / genre.
- **Hook generator** — AI rewrites first 3 seconds of user's script into 5 hook variants. Creator picks best. (First 3 sec = 80 % of retention on Reels.)
- **A/B publisher** — post variant A to IG Story, track engagement for 2 hours, auto-push the winning variant to Reel feed.
- **Creator coach** — weekly analytics dashboard: "Your reels hooked 30 % → median 45 %. Your sweet spot is 18-22 s. Try these 3 topics this week."
- **Collab / duet mode** — record a reply-reel to a friend's reel, voices auto-balanced.
- **Template marketplace** — pro creators publish reel templates (script + vibe + cut points). Earn 70 % revenue share. Flywheel: pros build templates → amateurs buy → money for pros → more templates.

Success metric: 10 % of users who ship a reel also use a growth tool.

### Phase 6 — Monetization Unlock (when WAU crosses 100 k)

- **Brand collab marketplace** — connect SMB advertisers (Zomato, Cred, local kirana brands) to niche creators. Take 10–15 % of deal value.
- **Affiliate code manager** — first-class support for IG native affiliate + YT affiliate, inside the editor. Paste product link → app shortens + tracks + auto-inserts into caption + reminds creator to add disclosure.
- **Pro subscription** — ₹199 / month (match InShot) unlocks: unlimited cloud processing, all voice effects, AI Ghostwriter, trending audio library, templates.
- **Credits for heavy users** — video > 60 s, multi-language auto-dub, ElevenLabs-grade voice clone. Pay-per-use on top of Pro.

## 4. "Wow" ideas specifically for Indian youth

These are launch-able as mode presets in Phase 3 / 4 without engineering whole new pipelines.

| Mode | What it does | Why it goes viral |
|---|---|---|
| **Cricket Commentary** | Adds stadium cheer + reverb + "commentator" intonation EQ | Every IPL season. Evergreen. |
| **Confession Mode** | Voice-change for anonymity + plain BG + auto-captions | Teenage emotional content is enormous; competitors don't serve it |
| **Study Vlog** | Lo-fi beat + rain + silence-trim + soft cam filter | Board-exam season = 200 M students |
| **Roast Battle** | Auto-detects tone → triggers laugh-track, sad-trombone, air-horn | Remix culture |
| **Mummy-Papa Safe** | Auto-beep inappropriate words + auto-blur face | Parents-check content; creators love this |
| **Bhakti Mode** | Temple bells + aarti BG + calm voice preset | Mass audience, 200 M+ spiritual content viewers |
| **Desi Podcast** | 2-mic capture, speaker separation, dual captions | 10 M+ aspiring podcasters in India |
| **Meme Dubbing** | Import viral meme clip, record your voice-over, auto lip-sync proxy | Meme accounts are IG's biggest growth driver |

## 5. What we keep vs what we cut

### Keep and lean into
- On-device RNNoise pipeline. Privacy story for sensitive creators (confession mode).
- Server Demucs extract + adaptive + quality guard. Moat on noisy-env cleaning.
- -16 LUFS loudness target. Already platform-compliant.
- Multi-variant reel output. Perfect fit for A/B publisher in Phase 5.
- BrandGradient + MD3 design system. Just landed; keep.

### Cut / deprioritize
- General-purpose "upload any audio, clean it, download WAV" flow. Useful but not unique; keep it but don't market it.
- Batch processing (Pro). Never worked, no users asking. Kill.
- "Lab DEV" bottom-nav item. Dev-only. Hide from release builds.
- Ad units (scaffolded but never placed). Decision: **no banner ads**. Pro subscription only. Protects UX for the creator segment who will pay.

### Add (near-term, not phased yet)
- **Referral program** — Pro for 1 month for every 3 friends who ship a reel. Cheaper than paid acquisition.
- **Language-first onboarding** — detect device locale, show UI + sample reels in that language. English-only onboarding is leaving tier-2/3 on the table.

## 6. Go-to-market

### Acquisition
1. **Campus ambassadors** — IIT / NIT / IIM / Symbiosis / Manipal content creator clubs. Free Pro lifetime + a monthly topic brief. They post, we get reach.
2. **Micro-influencer seeding** — 50 creators in 10-50 k follower range across Hindi / Marathi / Tamil. One-month free Pro + feature-feedback loop. Target: 5 of them post an organic "I found this app" reel.
3. **UGC challenges** — #BolsaafChallenge weekly topic. Winner gets ₹5 k + feature in the app.
4. **App Store Optimization** — target long-tail Hindi / Hinglish keywords: "awaaz saaf karne wala app", "reel banane ka app", "noise hatane wala app". English keywords are already saturated.

### Retention
- Weekly topic email / push: "This week's trending hook — try it in one tap".
- Creator coach dashboard (Phase 5).
- Template marketplace (Phase 5) creates a reason to come back weekly.

### Monetization ladder
- Free: 5 reels / month, watermark, basic voice clean, 3 backgrounds.
- Pro ₹199 / month: unlimited, no watermark, all voice effects, all languages.
- Pro+ ₹499 / month: multi-language auto-dub (per-minute credits included), template marketplace (free access), priority server queue.

### Partnerships worth exploring
- **ElevenLabs / Coqui / Sarvam AI** for multi-language voice clone. Revenue share or B2B API deal.
- **Saregama / Hungama** for licensed trending audio library. Revenue share on Pro subs.
- **Instagram Graph API / YouTube Data API** for direct publish + analytics.

## 7. Critical risks

| Risk | Mitigation |
|---|---|
| CapCut clones voice-first in 6 months | Move fast. Our moat is Indian noisy-env *specifically*. General CapCut users won't notice, but our target *will* feel the difference. Own that segment first, expand later. |
| Google Play rejects something (affiliate? voice clone?) | Stay 100 % within Play policy. No referral-auto-post, no unconsented voice clone (always require 30-sec verification record). |
| Server cost on voice-clone / auto-dub explodes | Pay-per-minute pricing from day one. Don't absorb the cost. |
| Language auto-caption accuracy is poor | Ship Hindi-first with manual correction UI. Expand to other languages only after Hindi hits 95 % WER on Indian accent dataset. |
| User confusion: "is this a voice app or a reel app?" | Rename the tagline: "The voice-first reel studio". Home screen leads with "Record a reel", not "Clean audio". |

## 8. Immediate next steps (pre-Phase-3)

Before any phased work starts, these must land:

1. **Finish v1.1.0 Play upload** — post v1.0.3 approval, ship the v1.1.0 AAB that's already sitting in `app/build/outputs/bundle/release/`. Don't let the current polish work rot.
2. **Decide billing provider** — Play Billing is the only compliant path. Wire up `PlanDialog`. Until billing works, Phase 3 has no revenue model.
3. **Pick ONE phase, not all.** My recommendation: Phase 3 first (Hindi captions + desi backgrounds + best-take + one-tap share) because it's the shortest path to a "whoa, I have to try this" moment for a new user.
4. **Recruit 5 beta users** from the target segment (tier-2 college students or early-stage creators). Weekly feedback calls during Phase 3 build.
5. **Reserve the domain / handle** — `bolsaaf.app`, `@bolsaafofficial` on IG / YT. Even if not used yet, squat so a competitor can't.

## 9. What this document is NOT

- Not a promise. Phases are time estimates, not commitments.
- Not exclusive of pivots. If beta-5 say "we want video editing, not voice dub", we pivot.
- Not a solo effort. Scaling to Phase 5 needs at least a designer + a growth marketer + a second Android dev. Plan hiring.
- Not about India forever. India first because we understand the user. Southeast Asia (Indonesia, Philippines, Bangladesh) in 2027 — same noisy-env voice advantage.

---

**Bottom line**: the world has enough visual video editors. Nobody is building a voice-first reel studio for the 1.4 B humans who don't record in a soundproof studio. That's us. Let's ship Phase 3 and see if the market agrees.
