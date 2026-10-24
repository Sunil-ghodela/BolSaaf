# ReelVoice — retention, email and web growth plan

Checked: 23 October 2026. Planning only: no campaign sent, no offer activated, no production changes made.

## What is verified

- User-provided Play screenshots cover 24 September–21 October (last 28 days). They show user acquisitions averaging 3.71/day, user loss averaging 2.64/day, total installs 139, daily active users averaging 2.5, and store-listing conversion averaging 56.6%. These are differently defined aggregates, not a cohort-retention calculation. Do not divide them to claim churn or retention.
- Latest-production-release card: 5 installs and 14.3% install base. The latter is version adoption, not a retained-user percentage. The new release has only recently gone live; these screenshots cannot establish its retention impact.
- Another summary shows 105 device acquisitions, 39 device first opens and 50 monthly active devices. These are not interchangeable with user metrics or the KPI-period averages. Investigate first-open measurement and period/definition alignment before claiming 66 users never opened the app.
- Crash/ANR cards say unavailable, not zero.
- Read-only production query of `apps.voice.models.VoiceUser`, 23 October at 05:22 UTC: 2 accounts, both with email, both created within the last 28 days. Account type (internal/test/customer) and verified ownership were NOT checked. Emails were not exported.
- VoiceUser fields have no marketing consent or unsubscribe record. This does not rule out consent elsewhere; an auditable opt-in source remains unverified. Install and account creation are not, by themselves, marketing permission.
- Google Play does not provide an email list of everyone who installs. Anonymous/local-only users cannot be identified through Play metrics.

## Deployed web: inspect this, not the older roadmap

- Landing: https://tranquilwaters.in/reelvoice/
- Account: https://tranquilwaters.in/reelvoice/account.html
- Studio: https://tranquilwaters.in/reelvoice/cleaning.html
- Account page has email registration/login, Google sign-in wiring, and session UI. Runtime login/purchase/processing success was not tested in this audit.
- Cleaning page imports auth, processing and analytics modules and gates the dashboard on sign-in. Anonymous local preview versus authenticated cloud processing needs a user-flow test, not an assumption.
- Landing's “Get ReelVoice” links to web studio, not Google Play. Bottom copy says the apps are “coming together” and uses an “Ask for access” mailto even though Android is live. Several web-studio CTAs are labelled `app-download` in conversion attributes: fix naming before interpreting conversion reports.
- The deployed analytics helper pushes events into `dataLayer` and optionally calls `gtag`. That alone does not prove events reach an analytics property or that useful reports exist.
- Privacy FAQ emphasises on-device processing without clearly explaining cloud jobs. Explain which action uploads media, its purpose/retention and local-only alternatives; do not send private media or identifiers to ad pixels.

## Priority 1 — know where people stop (days 1–3, proposed)

1. Run the current app's first-use path on a clean, disposable test install: permission -> record/pick -> clean -> compare -> save/share -> reopen History. Repeat denied permission, offline, quota reached, cancel and failed processing paths. Preserve real user media and accounts.
2. Define safe funnel events: first_open, record_start/stop, file_pick, processing_start/success/failure, comparison_play, export_success, share_action, signup_complete, opt_in/out. Share action is not proof a recipient received the media. No email, file names, raw audio, tokens or transcript content in analytics payloads.
3. Reuse existing telemetry where possible. Check collection/consent requirements and endpoint delivery. Keep account identifiers out of public ad/analytics events; use privacy-preserving first-party identifiers only where appropriate.
4. Produce acquisition-date cohorts with first successful result, D1/D7 return and a second successful result. Split app version and channel; do not merge web and Android anonymous devices as known people. Report numerator/denominator and allow the cohort to mature. Until measured, the reason for user loss is unknown.
5. Fix the earliest reproduced blocker first. Do not use email or offers as a substitute for a working first result. Do not force sign-in just to collect emails for local recording.

## Priority 2 — lawful, useful email audience (days 2–5, proposed)

- Check the two existing accounts privately for internal/test status and any separately recorded marketing permission. An approved admin export can be scoped to eligible accounts; do not infer install history from registration.
- Add an optional, unchecked checkbox to app/web account surfaces: “Email me ReelVoice tips, product news and offers.” Allow the product to work without this choice.
- Record consent timestamp, source, copy/version, purpose, confirmation status and withdrawal; consider double opt-in to verify ownership. Add unsubscribe and suppression handling before campaigns.
- Keep essential security/account/receipt messages separate from marketing. Do not relabel a promotional offer as a transactional update or send a campaign to an unverified-consent list.
- Configure an email provider, verified sender, SPF/DKIM/DMARC, bounce/complaint suppression, rate limits and plain-text fallback. Provider and spend are not selected or purchased by this plan.

## Priority 3 — web conversion (days 3–7, proposed)

- Present two clear actions: “Try on web” and “Get Android app”, with the latter linking to https://play.google.com/store/apps/details?id=com.reelvoice . iOS should say “Coming soon” while waiting for review, not claim availability.
- Replace obsolete early-access text. Make working features, server processing and quota/account requirements explicit.
- Add a short, honest before/after demo with permission-cleared audio; demonstrate the actual output, not an unsupported quality claim.
- Prefer an account-free local demo if technically supported; request account creation when needed for cloud jobs, quota or cross-device features. Verify the deployed flow first.
- Add optional update signup and one-question feedback: “What stopped you?” Do not block export or require a positive review.
- Separate measurements: demo start, demo completion, cloud job success, opt-in, actual store-link click and purchase verification. A store click is not an install.
- Audit current price/quota copy against server entitlements and app-store product setup before promoting any offer. Reuse the existing account model and entitlement contract.

## First campaign — only after opt-in and a tested flow

Proposed small pilot, not an approved launch:

1. Welcome: a short 20-second tutorial with one CTA to achieve a first clean result.
2. After ~2 days, only for consented users without a successful result: one helpful tip and a feedback link; exclude people who already completed the action.
3. After ~7 days, if still relevant: a single comeback message, optionally offering a proposed one-time 10-minute bonus. Confirm cost, expiry, abuse protection, server crediting and store-policy compliance before making the promise. No automatic subscription or charge.
4. Suppress recipients after unsubscribe, complaint, bounce or completed goal; avoid overlapping pilot messages. A beta reward must never depend on a positive rating/review.
5. Start with 5–10 eligible recipients if available; if fewer, use the actual number. Current database count does not support assuming a large list.

Measure delivered emails, clicks, subsequent successful results, D7 return and unsubscribes/complaints. Email opens are unreliable and not proof of engagement. For such small samples, treat results as qualitative rather than declaring a winning A/B test.

## Working order and gates

1. Audit/fix first-result and repeat-use flow + verify safe analytics.
2. Correct deployed-web messaging and CTA destinations.
3. Implement and test opt-in/unsubscribe + sender infrastructure.
4. Draft/update email and proposed incentive; approve audience, final copy and any spend before sending.
5. Pilot with eligible users; review actual repeat-success metrics before scaling paid acquisition.

No broad paid campaign, unsolicited email blast, purchase, production deployment or release action is authorised by this planning document.
