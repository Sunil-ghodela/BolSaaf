# BolSaaf Billing Contract — Play Billing ⇄ Server

> Date: 2026-10-16 (rev 3). Contract between Android client (`BillingManager`) and Django backend (`apps/voice/billing_views.py`). **Server endpoint not yet implemented** — this document is the spec. No billing code is written until this contract is approved.

## Status of this document

- **Structure and launch decisions approved by the owner (2026-10-16).** Implementation can proceed against this revision.
- **Implementation remains server-first:** do NOT ship client-only Pro unlocks; Play Console prices and server entitlements remain authoritative.
- **`pro_annual` is canonical** for the yearly product. The old `pro_yearly` was a placeholder only.
- **Pricing is not hardcoded in the app.** Play Console base plans hold the launch amounts and `ProductDetails` renders the runtime price. Approved launch prices are ₹79/month and ₹599/year; introductory offers are off for launch.
- Product IDs, entitlement keys, and response shapes below are the stable contract. Renaming them after launch strands existing subscribers or breaks the client.

## Why a server at all

Play Billing returns a signed purchase token to the client. We must NOT trust client-side boolean flags to unlock Pro — a modified APK can fake them. The server is the authority:

1. Client obtains `purchaseToken` from Play.
2. Client POSTs `{product_id, purchase_token, order_id?}` to `/voice/billing/validate/`.
3. Server calls Google Play Developer API to verify the token against the real subscription state.
4. Server persists the verified period (`pro_expires_at`) and renewal state.
5. Client re-fetches `/voice/auth/me/` and picks up `is_pro: true` + entitlements.

If steps 3–4 fail or are skipped, the user is NOT Pro, regardless of what the client thinks. That invariant is load-bearing — everything else is optimisation.

---

## 1. Product catalogue (Play Console → Monetization → Subscriptions)

| Product ID | Display name | Base plan period | Price | Status |
|---|---|---|---|---|
| `pro_monthly` | BolSaaf Pro | 1 month | ₹79 launch price | Create in Play Console before implementation |
| `pro_annual` | BolSaaf Pro (Annual) | 1 year | ₹599 launch price | Create in Play Console before implementation |

Rules:

- Product IDs are **immutable**. `pro_monthly` is already referenced in `BillingManager`; keep it. `pro_annual` replaces the earlier placeholder `pro_yearly` naming — update the `BillingManager` doc comment when implementation starts.
- **Monthly and Annual grant identical entitlements.** The only difference is price, billing period, and renewal cadence. Do not fork the feature set per period — that doubles the support matrix for zero user value.
- Display price is always read from `ProductDetails.subscriptionOfferDetails` at runtime. Never hardcode `₹199`/`₹299`/`₹1,499` in client copy or server responses.
- Annual discount, introductory offers, and any promo are Play Console base-plan/offer config, not code.

---

## 2. Entitlements (source of truth)

Entitlements are **named capability keys** with stable string IDs. The client UI, the server, and the future landing page all consume the same keys — never maintain a second pricing/capability list elsewhere.

Server returns an `entitlements` object on `/voice/auth/me/` (see §4). The client renders from it; it does not invent limits locally.

| Entitlement key | Type | Free | Pro (Monthly / Annual) | Notes |
|---|---|---|---|---|
| `voice_minutes_monthly` | int \| null | `20` | `null` (= unlimited) | `null` means unlimited; never a sentinel like `-1` |
| `cleaning_modes` | string[] | `["basic","standard"]` | `["basic","standard","studio","pro"]` | `studio`/`pro` = DeepFilterNet quality |
| `video_processing` | bool | `true` (capped) | `true` | Free may use video but with caps below |
| `video_export_watermark` | bool | `true` | `false` | Pro exports clean video |
| `video_minutes_monthly` | int \| null | `10` | `60` | Applies to video jobs; expand after usage review |
| `max_audio_upload_mb` | int | `25` | `100` | Client currently caps at 25 MB (`FASTLIB_MAX_AUDIO_BYTES`) |
| `max_video_upload_mb` | int | `50` | `250` | Client currently caps at 50 MB (`FASTLIB_MAX_VIDEO_BYTES`) |
| `reel_presets` | string[] | basic set | full set (`Podcast`, `Rain`, `Cafe`, `Viral`) | Server-defined preset names |
| `priority_queue` | bool | `false` | `true` | Faster job scheduling |
| `ad_free` | bool | `false` | `true` | Hides ad slots when ads ship |
| `extract_voice` | bool | `true` | `true` | Core capability, both tiers |
| `add_background` | bool | `true` (limited catalog) | `true` (full catalog) | Free sees a subset of `/voice/backgrounds/` |

### Audio vs video grouping

- **Audio entitlements** (`voice_minutes_monthly`, `cleaning_modes`, `max_audio_upload_mb`, `extract_voice`, `add_background`, `reel_presets`, `priority_queue`, `ad_free`) map to the `CLEAN`, `ADD_BACKGROUND`, `REEL_MODE` flows.
- **Video entitlements** (`video_processing`, `video_export_watermark`, `video_minutes_monthly`, `max_video_upload_mb`) map to the `VIDEO_PROCESS` flow and `reel/create/` video output.
- Video and audio usage are metered **separately** (`voice_minutes_monthly` vs `video_minutes_monthly`) so a heavy video user can't exhaust audio quota silently.

### Free quota semantics

- Free quota is per calendar month, keyed by `free_period_yyyymm` (already in the model + server reset logic in `auth_views._maybe_reset_period`).
- Free launch values are config on the server (`VoiceUser.free_quota_minutes`), not client constants: 20 audio minutes and 10 video minutes per calendar month. The client's hardcoded fallback (`FREE_QUOTA_MINUTES = 8` in `MainActivity`) must be replaced by the server value once signed in.
- Pro users never consume quota (`ConsumeQuotaView` already short-circuits on `is_pro`).

---

## 3. Server validation

### `POST /voice/billing/validate/`

Request (JSON, requires existing auth token):

```json
{
  "product_id": "pro_monthly",
  "purchase_token": "ajfj...gKLm",
  "order_id": "GPA.3367-1234-5678-00001"
}
```

- `product_id` ∈ {`pro_monthly`, `pro_annual`}.
- `purchase_token` from `Purchase.getPurchaseToken()`.
- `order_id` from `Purchase.getOrderId()` — optional, used for refund reconciliation.

Headers:
- `Authorization: Bearer <voice auth token>` — the purchase is tied to the currently logged-in `VoiceUser`.
- `Content-Type: application/json`

### Responses

Success (200):

```json
{
  "status": "ok",
  "is_pro": true,
  "plan": "pro_monthly",
  "pro_expires_at": "2026-05-16T12:00:00Z",
  "auto_renewing": true,
  "entitlements": { "voice_minutes_monthly": null, "video_export_watermark": false, "...": "..." }
}
```

Already-valid replay (client resends a token we already validated):

```json
{
  "status": "already_active",
  "is_pro": true,
  "plan": "pro_monthly",
  "pro_expires_at": "2026-05-16T12:00:00Z",
  "auto_renewing": true
}
```

Validation failed (400):

```json
{ "status": "invalid", "reason": "token_expired" | "token_not_found" | "product_mismatch" | "user_mismatch" }
```

Google Play API error (502):

```json
{ "status": "upstream_error", "reason": "play_api_5xx", "retry_after_seconds": 30 }
```

### Server-side flow

```python
# apps/voice/billing_views.py  (SKETCH — not to be written until contract approved)

from googleapiclient.discovery import build
from google.oauth2 import service_account

SERVICE_ACCOUNT_FILE = "/var/secrets/bolsaaf-play-sa.json"
PACKAGE_NAME = "com.reelvoice"          # matches applicationId in app/build.gradle.kts
VALID_PRODUCT_IDS = {"pro_monthly", "pro_annual"}

def _play_client():
    creds = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE,
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    return build("androidpublisher", "v3", credentials=creds)

def validate_purchase(request):
    user = authenticate_bearer(request)
    if user is None:
        return JsonResponse({"status": "invalid", "reason": "auth_required"}, status=401)

    body = json.loads(request.body)
    product_id = body["product_id"]
    purchase_token = body["purchase_token"]
    if product_id not in VALID_PRODUCT_IDS:
        return JsonResponse({"status": "invalid", "reason": "product_mismatch"}, status=400)

    client = _play_client()
    try:
        result = client.purchases().subscriptionsv2().get(
            packageName=PACKAGE_NAME, token=purchase_token,
        ).execute()
    except HttpError as e:
        return JsonResponse(
            {"status": "upstream_error", "reason": f"play_api_{e.resp.status}",
             "retry_after_seconds": 30}, status=502,
        )

    line_items = result.get("lineItems", [])
    if not any(li.get("productId") == product_id for li in line_items):
        return JsonResponse({"status": "invalid", "reason": "product_mismatch"}, status=400)

    state = result.get("subscriptionState")
    if state not in ("SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"):
        return JsonResponse({"status": "invalid", "reason": f"state_{state}"}, status=400)

    expiry_iso = result["lineItems"][0]["expiryTime"]
    user.pro_expires_at = parse_iso(expiry_iso)
    user.subscription_product_id = product_id
    user.subscription_state = state
    user.auto_renewing = result.get("autoRenewingFlag", False)
    user.last_verified_at = timezone.now()
    user.save(update_fields=[...])

    # Acknowledge server-side too (idempotent) so a client kill mid-ack can't force a refund.
    client.purchases().subscriptionsv2().acknowledge(
        packageName=PACKAGE_NAME, token=purchase_token, body={},
    ).execute()

    return JsonResponse({"status": "ok", "is_pro": True, "plan": product_id,
                         "pro_expires_at": expiry_iso,
                         "auto_renewing": user.auto_renewing,
                         "entitlements": build_entitlements(user)})
```

### Required schema additions (`VoiceUser`)

Current fields: `is_pro`, `pro_expires_at`, `free_quota_minutes`, `free_minutes_left`, `free_period_yyyymm`.
Add:

| Field | Type | Purpose |
|---|---|---|
| `subscription_product_id` | CharField | `pro_monthly` / `pro_annual` |
| `subscription_state` | CharField | Latest Play `subscriptionState` |
| `auto_renewing` | BooleanField | Mirrors Play `autoRenewingFlag` |
| `last_verified_at` | DateTimeField | Last successful Play verification |
| `purchase_token_hash` | CharField | SHA-256 of token for audit; **never store raw token** |

`is_pro` should be **derived at read time** as `pro_expires_at > now()` rather than a stored bool that can drift. Keep the column for query convenience but recompute in a nightly job and on every `/me/` read.

---

## 4. Client-facing profile (`GET /voice/auth/me/`)

Extend the existing `_user_to_dict` payload with the entitlement block so the client has a single hydration source:

```json
{
  "user": {
    "id": 42, "email": "a@b.com", "display_name": "...", "handle": "...",
    "is_pro": true,
    "plan": "pro_monthly",
    "pro_expires_at": "2026-05-16T12:00:00Z",
    "auto_renewing": true,
    "free_quota_minutes": 20, "free_minutes_left": 20, "free_period_yyyymm": "202604",
    "entitlements": {
      "voice_minutes_monthly": null,
      "cleaning_modes": ["basic", "standard", "studio", "pro"],
      "video_processing": true,
      "video_export_watermark": false,
      "video_minutes_monthly": null,
      "max_audio_upload_mb": 100,
      "max_video_upload_mb": 250,
      "reel_presets": ["Podcast", "Rain", "Cafe", "Viral"],
      "priority_queue": true,
      "ad_free": true,
      "extract_voice": true,
      "add_background": true
    }
  }
}
```

The client **must** read limits and feature flags from `entitlements`, not from local constants. `AuthApi.AuthUser` gains `plan`, `autoRenewing`, and an `entitlements` map.

---

## 5. Restore purchases

Restore is implicit because entitlements derive from the server, not local state.

1. On every app launch, `BillingManager.connect { refreshActivePurchases() }` (already wired in `MainActivity`).
2. For each active Play purchase, POST to `/voice/billing/validate/` — the server re-verifies and refreshes `pro_expires_at`.
3. Call `/voice/auth/me/` to hydrate the UI.
4. Device switch / reinstall: Play returns the same subscription to the new install; step 1–3 restores Pro automatically.
5. Optional explicit "Restore purchases" button in Profile: same as step 1–3, plus a toast ("Restored" / "No active subscription found").

Never persist `isProMember = true` in SharedPreferences as truth. It may be cached for instant UI paint, but must be reconciled with `/me/` on each launch.

---

## 6. Expiry

- **Definition:** Pro is active while `pro_expires_at > now()`. `is_pro` is a projection of that comparison.
- **Renewal:** Play extends the subscription; a nightly reconciliation job calls `purchases.subscriptionsv2.get()` for every user with `auto_renewing = true` and bumps `pro_expires_at` (RTDN is the Phase-5 realtime alternative).
- **Grace period:** if Play reports `SUBSCRIPTION_STATE_IN_GRACE_PERIOD`, keep Pro active until `expiryTime` (Play retries billing). Do not revoke.
- **Account hold:** `SUBSCRIPTION_STATE_ON_HOLD` → treat as active only while `pro_expires_at > now()`; no new renewal.
- **Lapse:** when `pro_expires_at` passes without renewal, `/me/` reports `is_pro: false`; user reverts to free quota and free entitlements automatically.
- **Clock safety:** all comparisons use server time, never device time.

Nightly job (MVP alternative to RTDN):

```python
# for each VoiceUser with pro_expires_at set:
#   re-query Play by stored token; update pro_expires_at / subscription_state / auto_renewing
#   if pro_expires_at < now(): is_pro = False
```

---

## 7. Cancellation

- A user cancels in the Play Store. Play sets `autoRenewingFlag = false` but keeps the subscription valid until `expiryTime`.
- **We do NOT revoke on cancel.** `pro_expires_at` still applies — the user keeps Pro for the paid period. `/me/` returns `is_pro: true`, `auto_renewing: false`.
- The client surfaces "Renews until <date> — cancelled" copy using `auto_renewing = false` + `pro_expires_at`.
- On the eventual `SUBSCRIPTION_STATE_EXPIRED` / RTDN `SUBSCRIPTION_EXPIRED`, `/me/` flips to `is_pro: false` and free entitlements apply.

### Refunds / revocation

- User or Play revokes the purchase → RTDN `SUBSCRIPTION_REVOKED` (or Play API reports revoked state).
- Server revokes **immediately**: `pro_expires_at = now()`, `auto_renewing = false`.
- Next `/me/` shows `is_pro: false`; Pro features lock, free quota applies.

### RTDN (Phase-5 optimisation)

Play pushes to a Pub/Sub topic for: renewal, cancel, expire, revoke. For MVP skip RTDN and rely on the nightly poll + validate-on-launch. RTDN removes up-to-24h staleness and should follow shortly after launch.

---

## 8. Client responsibilities

1. `refreshActivePurchases()` on every app launch — catches cross-device / reinstall subscriptions.
2. On `onPurchaseSuccess`, POST token + productId to `/voice/billing/validate/`. **Do not flip `isProMember` locally.**
3. After a 200 from the server, call `/voice/auth/me/` to refresh the profile + entitlements.
4. Remove the current `validatePurchaseWithServer` stub in `MainActivity` that flips local state — it is a closed-testing convenience and must not ship.
5. Read every feature gate and size cap from `entitlements`; delete local Pro assumptions from `ProfileScreen` (e.g. the hardcoded `₹299` card and `Free up to 8 min` copy must come from server/Play data).
6. Handle `BillingResponseCode.ITEM_ALREADY_OWNED` by re-validating, not by erroring.

---

## 9. Security notes

- **Never log the full `purchase_token`.** Store only `purchase_token_hash` (SHA-256) for audit.
- **Rate limit** `/voice/billing/validate/` to ~10 calls/min/user.
- **Service account JSON** at `/var/secrets/bolsaaf-play-sa.json`, mode `400`, owned by the gunicorn user. Add the SA email in Play Console → API access with financial/order permissions. NEVER commit.
- **Acknowledgement is idempotent** — server and client both calling it is safe; validate before acknowledging so a fraudulent token isn't auto-consumed.
- Validate `product_id` against the allow-list before hitting Play.

---

## 10. Implementation readiness (approved launch decisions)

Launch decisions are now set: ₹79/month, ₹599/year, 20 free audio minutes/month, 10 free video minutes/month, and 60 Pro video minutes/month. Execute the implementation in this order.

1. **Play Console:** create base plans for `pro_monthly` and `pro_annual` with the finalized prices/offers. Product IDs are immutable.
2. **Server schema:** add the `VoiceUser` fields in §3 (`subscription_product_id`, `subscription_state`, `auto_renewing`, `last_verified_at`, `purchase_token_hash`) via migration; derive `is_pro` from `pro_expires_at` at read time.
3. **Server endpoint:** implement `/voice/billing/validate/` per §3; add the route in `apps/voice/urls.py`; configure the Play service account (§9).
4. **Server entitlements:** implement `build_entitlements(user)` and return it from `/voice/auth/me/` (§2, §4) using the finalized quota values. `pro_monthly` and `pro_annual` map to the same entitlement set.
5. **Client — remove client-only Pro unlock:** delete `MainActivity.validatePurchaseWithServer`'s local `isProMember = true`; replace it with the real POST → `/me/` refresh. This stub must never ship.
6. **Client — server-validated flow:** wire `BillingManager.onPurchaseSuccess` → validate endpoint → `/me/` (§8). Add `pro_annual` alongside `pro_monthly` in the product query.
7. **Client — entitlement-driven UI:** `AuthApi.AuthUser` gains `plan`, `autoRenewing`, `entitlements`; `ProfileScreen.PlanDialog` reads price from `ProductDetails` and limits from `entitlements`; drop hardcoded `₹299` / `8 min` copy.
8. **Verify:** license-tester purchase → server sets `pro_expires_at` → `/me/` reports `is_pro` → restart app restores Pro → cancel keeps Pro to period end → expiry reverts to Free.

### Ready-state guarantees

- No client code grants Pro on its own — the only path to `is_pro: true` is a server-verified token (§“Why a server at all”).
- Entitlement keys in §2 are the single shared vocabulary for client, server, and the landing page.

---

## 11. Open questions (owner decisions before implementation)

1. **Pricing:** monthly amount, annual amount, annual discount %, and whether to run an introductory offer. Not coded — set in Play Console.
2. **Free quota values:** `voice_minutes_monthly` and `video_minutes_monthly` for Free.
3. **Grace period UX:** on a server 502 after a successful client purchase, grant a 24h provisional Pro while retrying? Google recommends yes — confirm.
4. **Family sharing:** Play default is ON. Disable or keep?
5. **Ads:** do we ship ads at all in the near term? `ad_free` entitlement only matters once ads exist.
6. ~~**`pro_annual` naming**~~ — **resolved:** `pro_annual` is canonical (owner-confirmed).

---

## Related files

- Client billing: `app/src/main/java/com/reelvoice/billing/BillingManager.kt`
- Wire-up + stub to remove: `app/src/main/java/com/reelvoice/MainActivity.kt::startProPurchase`, `validatePurchaseWithServer`
- Auth + profile client: `app/src/main/java/com/reelvoice/audio/AuthApi.kt` (existing `AuthUser.isPro`; add entitlements)
- Plan UI to de-hardcode: `app/src/main/java/com/reelvoice/ui/screens/ProfileScreen.kt::PlanDialog`
- Server stub (TODO): `/var/www/simplelms/backend/apps/voice/billing_views.py`
- Server auth/profile: `/var/www/simplelms/backend/apps/voice/auth_views.py`
- Server model + required new fields: `/var/www/simplelms/backend/apps/voice/models.py::VoiceUser`
- Server routes: `/var/www/simplelms/backend/apps/voice/urls.py`
