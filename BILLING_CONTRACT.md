# BolSaaf Billing Contract — Play Billing ⇄ Server

> Date: 2026-04-16. Contract between Android client (`BillingManager`) and Django backend (`apps/voice/billing_views.py`). **Server endpoint not yet implemented** — this document is the spec.

## Why a server at all

Play Billing returns a signed purchase token to the client. We must NOT trust client-side boolean flags to unlock Pro — a modified APK can fake them. The server is the authority:

1. Client obtains `purchaseToken` from Play.
2. Client POSTs `{productId, purchaseToken}` to `/voice/billing/validate/`.
3. Server calls Google Play Developer API to verify the token.
4. Server sets `VoiceUser.pro_expires_at` based on the verified subscription period.
5. Client re-fetches `/voice/auth/me/` and picks up `is_pro: true`.

If steps 3–4 fail or are skipped, the user is NOT Pro, regardless of what the client thinks. That invariant is load-bearing — everything else is optimisation.

## Product catalogue (Play Console → Monetization → Subscriptions)

| Product ID | Display name | Base plan period | Price (INR) | Status |
|---|---|---|---|---|
| `pro_monthly` | BolSaaf Pro | 1 month | ₹199 | **Create now** |
| `pro_yearly` | BolSaaf Pro (Yearly) | 1 year | ₹1,499 | Defer to Phase 5 |

Keep the product IDs stable forever. Renaming them in Play Console is a breaking change that strands existing subscribers.

## Client → Server

### `POST /voice/billing/validate/`

Request (JSON, requires existing auth token):

```json
{
  "product_id": "pro_monthly",
  "purchase_token": "ajfj...gKLm",   // from Purchase.getPurchaseToken()
  "order_id": "GPA.3367-1234-5678-00001"  // from Purchase.getOrderId() — optional but useful for refund handling
}
```

Headers:
- `Authorization: Bearer <voice auth token>` — the purchase is tied to the currently logged-in VoiceUser.
- `Content-Type: application/json`

### Response

Success (200):

```json
{
  "status": "ok",
  "is_pro": true,
  "pro_expires_at": "2026-05-16T12:00:00Z",
  "auto_renewing": true,
  "acknowledged": true
}
```

Already-valid (purchase replay — client resends a token we already validated):

```json
{
  "status": "already_active",
  "is_pro": true,
  "pro_expires_at": "2026-05-16T12:00:00Z"
}
```

Validation failed (400):

```json
{
  "status": "invalid",
  "reason": "token_expired" | "token_not_found" | "product_mismatch" | "user_mismatch"
}
```

Google Play API error (502):

```json
{
  "status": "upstream_error",
  "reason": "play_api_5xx",
  "retry_after_seconds": 30
}
```

## Server-side implementation sketch

### Dependencies
- `google-api-python-client`
- `google-auth`
- Service account with `androidpublisher` scope (create in Google Cloud Console → IAM → Service Accounts, grant in Play Console → API access).

### Flow

```python
# apps/voice/billing_views.py

from googleapiclient.discovery import build
from google.oauth2 import service_account

SERVICE_ACCOUNT_FILE = "/var/secrets/bolsaaf-play-sa.json"
PACKAGE_NAME = "com.bolsaaf"

def _play_client():
    creds = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE,
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    return build("androidpublisher", "v3", credentials=creds)

@csrf_exempt
@require_POST
def validate_purchase(request):
    user = authenticate_bearer(request)
    if user is None:
        return JsonResponse({"status": "invalid", "reason": "auth_required"}, status=401)

    body = json.loads(request.body)
    product_id = body["product_id"]
    purchase_token = body["purchase_token"]

    client = _play_client()
    try:
        result = client.purchases().subscriptionsv2().get(
            packageName=PACKAGE_NAME,
            token=purchase_token,
        ).execute()
    except HttpError as e:
        return JsonResponse(
            {"status": "upstream_error", "reason": f"play_api_{e.resp.status}"},
            status=502,
        )

    # Check the subscription is active and for our product
    line_items = result.get("lineItems", [])
    if not any(li.get("productId") == product_id for li in line_items):
        return JsonResponse({"status": "invalid", "reason": "product_mismatch"}, status=400)

    state = result.get("subscriptionState")
    if state not in ("SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"):
        return JsonResponse({"status": "invalid", "reason": f"state_{state}"}, status=400)

    expiry_iso = result["lineItems"][0]["expiryTime"]  # RFC 3339
    auto_renewing = result.get("autoRenewingFlag", False)

    user.pro_expires_at = parse_iso(expiry_iso)
    user.save(update_fields=["pro_expires_at"])

    # Ack on Play side so we don't get auto-refunded. Client also acks, but server-ack is idempotent
    # and protects against client-kill mid-ack.
    client.purchases().subscriptionsv2().acknowledge(
        packageName=PACKAGE_NAME,
        token=purchase_token,
        body={},
    ).execute()

    return JsonResponse({
        "status": "ok",
        "is_pro": True,
        "pro_expires_at": expiry_iso,
        "auto_renewing": auto_renewing,
        "acknowledged": True,
    })
```

### Real-time Developer Notifications (RTDN)

Play will push a notification to a configured Pub/Sub topic whenever:
- Subscription renews (we extend `pro_expires_at`).
- Subscription is cancelled (we do NOT revoke; `pro_expires_at` still applies — user keeps Pro until the paid period ends).
- Subscription expires (we let the nightly cron transition `is_pro` to false).
- Subscription is refunded by the user / by Play (revoke immediately).

For MVP we can skip RTDN and poll `subscriptionsv2().get()` nightly on every Pro user. RTDN is a Phase-5 optimisation.

## Client responsibilities

1. `BillingManager.refreshActivePurchases()` on every app launch — catches subscriptions that were purchased on another device.
2. On `onPurchaseSuccess` callback, POST to `/voice/billing/validate/` with the token + productId.
3. After a 200 from the server, call `/voice/auth/me/` to refresh the user profile. This flows `is_pro: true` back to the UI.
4. Do NOT flip `isProMember = true` locally without the server confirmation. The current `validatePurchaseWithServer` stub in `MainActivity` does this for testing convenience — **remove before production**.

## Refund handling

- User requests refund via Google Play Store → Play pushes an RTDN (`ONE_TIME_PRODUCT_CANCELED` / subscription `SUBSCRIPTION_REVOKED`).
- Server revokes immediately: `pro_expires_at = now()`.
- Next `/voice/auth/me/` call shows `is_pro: false`.
- In-app Pro features lock; user sees normal quota again.

## Testing

### License testing (free test purchases)
1. Add the test Gmail account in Play Console → Setup → License testing.
2. Release the app to Internal testing track.
3. Sign in on the device with the test account.
4. Purchase flow works end-to-end without charging the card.

### Sandbox cards
Google Play does not support sandbox card numbers in India. Use the license-testing flow above.

## Security notes

- **Do not log `purchase_token` in full** on the server. Store hashed if needed for audit. The token alone lets anyone claim that subscription.
- **Rate limit** `/voice/billing/validate/` — 10 calls / minute / user should be plenty.
- **Service account JSON** lives at `/var/secrets/` with mode 400, owned by the `gunicorn` user. NEVER commit.
- **Acknowledgement is idempotent** — server-side and client-side both calling it is safe. But validate tokens before acknowledging so we don't auto-consume a fraudulent token.

## Open questions (decide before Phase 4)

1. **Grace period**: if server validation 502s after a successful client purchase, should we grant a 24h provisional Pro while we retry? Google recommends yes; UX team to confirm.
2. **Yearly plan** pricing: ₹1,499 (~2 months free vs monthly) or ₹1,799 (1 month free, higher margin)?
3. **Family sharing**: Play supports it for subscriptions. Default is ON. Do we want to disable?
4. **Introductory offer**: "First month ₹49" — Play supports promo offers natively. Worth trying once we have baseline conversion rate.

## Related files

- Client: `app/src/main/java/com/bolsaaf/billing/BillingManager.kt`
- Wire-up: `app/src/main/java/com/bolsaaf/MainActivity.kt::startProPurchase`, `validatePurchaseWithServer`
- Auth hand-off: `app/src/main/java/com/bolsaaf/audio/AuthApi.kt` (existing `AuthUser.isPro`)
- Server stub (TODO): `/var/www/simplelms/backend/apps/voice/billing_views.py`
