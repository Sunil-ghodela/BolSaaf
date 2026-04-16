package com.bolsaaf.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Phase 3 — Google Play Billing client for the Pro subscription.
 *
 * Product catalogue (Play Console):
 *   - `pro_monthly` — Pro tier monthly subscription (₹199 or market-localised).
 *   - `pro_yearly`  — (planned) ~₹1,499 or localised. Not yet offered in-app.
 *
 * Purchase flow:
 *   1. connect() — binds to Play services.
 *   2. queryProductDetails() — pulls current price + offer details for [PRODUCT_PRO_MONTHLY].
 *   3. launchPurchaseFlow(activity) — opens Play's native purchase UI.
 *   4. PurchasesUpdatedListener receives the result, we call [onPurchaseSuccess] with the token.
 *   5. Caller is expected to POST the token + productId to /voice/billing/validate/ on the server
 *      (see BILLING_CONTRACT.md). Server validates with Play Developer API, returns pro_expires_at.
 *   6. On server-confirmed valid, we acknowledge() the purchase so Play marks it consumed. If we
 *      never acknowledge within 3 days, Play auto-refunds the user.
 *
 * Failure modes callers should surface to the user:
 *   - BillingResponseCode.USER_CANCELED — silent; user tapped back.
 *   - BillingResponseCode.ITEM_ALREADY_OWNED — already Pro; just refresh profile.
 *   - any other code — show "Purchase failed — try again or contact support."
 */
class BillingManager(
    context: Context,
    private val onPurchaseSuccess: (productId: String, purchaseToken: String) -> Unit,
    private val onPurchaseError: (code: Int, message: String) -> Unit,
) {
    companion object {
        private const val TAG = "BillingManager"

        /** Must match the subscription product id configured in Play Console → Monetization → Subscriptions. */
        const val PRODUCT_PRO_MONTHLY = "pro_monthly"
    }

    private var proMonthlyDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "Purchase flow cancelled by user")
                // Silent — don't nag the user
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.w(TAG, "Item already owned — re-querying active purchases")
                refreshActivePurchases()
                onPurchaseError(result.responseCode, "You're already Pro — refreshing your profile.")
            }
            else -> {
                Log.w(TAG, "Billing error ${result.responseCode}: ${result.debugMessage}")
                onPurchaseError(result.responseCode, result.debugMessage)
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .setListener(purchasesUpdatedListener)
        .build()

    /** Connect to Play Services. Idempotent — calling twice is safe. */
    fun connect(onReady: () -> Unit = {}) {
        if (billingClient.isReady) {
            onReady()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing client connected")
                    queryProductDetails { onReady() }
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    onPurchaseError(billingResult.responseCode, billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected — will retry on next connect()")
            }
        })
    }

    fun disconnect() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    /** Cached ProductDetails so the UI can show a real localised price ("₹199 / month"). */
    fun proMonthlyDetails(): ProductDetails? = proMonthlyDetails

    /** Pulls Pro subscription ProductDetails from Play. */
    private fun queryProductDetails(onDone: () -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_PRO_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { result, products ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                proMonthlyDetails = products.firstOrNull { it.productId == PRODUCT_PRO_MONTHLY }
                if (proMonthlyDetails == null) {
                    Log.w(TAG, "Pro monthly product not found in Play response")
                }
            } else {
                Log.w(TAG, "queryProductDetailsAsync failed: ${result.debugMessage}")
            }
            onDone()
        }
    }

    /** Call from an Activity to open the Play purchase UI. [connect] must have completed first. */
    fun launchPurchaseFlow(activity: Activity): Boolean {
        val details = proMonthlyDetails ?: run {
            onPurchaseError(-1, "Pricing not available yet — please retry in a moment.")
            return false
        }
        // Subscriptions have "offers"; we take the first (base plan). Merchants can add
        // promo offers in Play Console — picking the first one is a sensible default.
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            onPurchaseError(-1, "No subscription offer available for this account.")
            return false
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onPurchaseError(result.responseCode, result.debugMessage)
            return false
        }
        return true
    }

    /**
     * Check Play for already-active Pro subscriptions (e.g. user reinstalled, switched devices,
     * or bought Pro in a previous session that we never managed to acknowledge).
     *
     * Safe to call on every app launch.
     */
    fun refreshActivePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            // PENDING (e.g. UPI that hasn't settled) is expected; Play will push an update later.
            return
        }
        val productId = purchase.products.firstOrNull() ?: return

        // Hand off to server validation. Server is responsible for calling
        // Play Developer API to confirm the token + extending pro_expires_at.
        onPurchaseSuccess(productId, purchase.purchaseToken)

        // Acknowledge to Play. If we don't acknowledge within 3 days, Play auto-refunds.
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { ackResult ->
                if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Ack failed: ${ackResult.debugMessage}")
                }
            }
        }
    }
}
