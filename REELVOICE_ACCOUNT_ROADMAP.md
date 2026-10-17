# ReelVoice account and web roadmap

Updated: 2026-10-17

## Current state

- Android already has real email/password auth: register, sign in, sign out, password reset, token persistence, and `/voice/auth/me/` session restore.
- The backend already exposes auth and entitlement endpoints. Subscription state is server-owned.
- iOS has an Account card and sign-in placeholder, but the auth calls are not wired yet.
- The ReelVoice website is a public landing page and studio preview. It has no web account session or dashboard.
- The landing page is deployed at `https://tranquilwaters.in/reelvoice/`.

## Recommended order

1. **Freeze the contract.** Use the existing backend auth vocabulary and entitlement response as the single source of truth. Do not create a second web-only user model or duplicate plan limits.
2. **Finish Android billing setup.** Complete Play Console base plans, service-account access, and one license-tester purchase. Keep Pro unlock server-validated.
3. **Wire iOS auth.** Reuse the same API base URL and request/response contract as Android. Add restore-session, sign-in, sign-up, logout, reset-password, and account deletion states.
4. **Define the web account scope.** Add web login only when cloud processing, cross-device history, or subscription management is enabled. The current client-side preview can remain usable without an account.
5. **Build the web account flow.** Add sign in, create account, reset password, session expiry, profile/plan page, and a clear sign-out action. Use the backend token contract; never trust a browser-only Pro flag.
6. **Connect web audio/video jobs.** Require authentication at upload/processing time, show quota and entitlement state from the server, and make job history account-scoped.
7. **Add analytics and ads carefully.** Keep Google Ads on public marketing pages. Do not send raw audio/video, auth tokens, or private account data to ad or analytics payloads.

## Acceptance criteria

- One account works across Android, iOS, and web.
- Entitlements and limits are returned by the backend and rendered consistently.
- Sign-out clears local tokens and protected pages/jobs stop working immediately.
- Expired sessions return the user to sign-in without losing local files.
- Account deletion and privacy-policy links are reachable from every account surface.
- Existing no-account, on-device recording and preview flows continue to work.

## Out of scope for the first web pass

- Social login unless the backend contract is explicitly extended.
- Rebuilding the Android auth backend.
- Making sign-in mandatory for the public landing page.
- Duplicating billing prices or quotas in website copy.
