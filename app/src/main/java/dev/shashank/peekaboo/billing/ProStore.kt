package dev.shashank.peekaboo.billing

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
import dev.shashank.peekaboo.BuildConfig
import dev.shashank.peekaboo.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One way to pay for Pro, as Google Play offers it to this user right now. */
data class ProPlan(
    val details: ProductDetails,
    val basePlanId: String,
    val offerToken: String,
    /** Recurring price, already formatted for the user's currency. */
    val price: String,
    val priceMicros: Long,
    val currency: String,
    /** ISO-8601 billing period of the recurring price, e.g. P1M or P1Y. */
    val period: String,
    /** Length of the free trial in days, if this offer starts with one. */
    val trialDays: Int?,
) {
    val yearly: Boolean get() = period == "P1Y"
    val monthlyMicros: Long get() = if (yearly) priceMicros / 12 else priceMicros
}

enum class StoreStatus { Connecting, Ready, Unavailable }

/**
 * Peek-a-Boo Pro: a Google Play auto-renewing subscription ([PRODUCT_ID]) with a monthly and a
 * yearly base plan. Pricing, trials and regional prices all live in the Play Console; the app
 * only reads what Play offers and unlocks Pro while a purchase is active.
 *
 * Billing talks to the Play Store app over IPC, so the app itself still needs no internet
 * permission.
 */
class ProStore(private val context: Context, private val settings: SettingsRepository) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val entitled = MutableStateFlow<Boolean?>(null)
    private val _plans = MutableStateFlow<List<ProPlan>>(emptyList())
    private val _status = MutableStateFlow(StoreStatus.Connecting)
    private val _busy = MutableStateFlow(false)

    val plans: StateFlow<List<ProPlan>> = _plans.asStateFlow()
    val status: StateFlow<StoreStatus> = _status.asStateFlow()
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /**
     * True while Pro is unlocked. Until Play has answered, the last known answer is used so the
     * guard doesn't flash Pro features off on every start.
     */
    val isPro: StateFlow<Boolean> = combine(entitled, settings.proCached) { live, cached ->
        BuildConfig.PRO_UNLOCKED || (live ?: cached)
    }.stateIn(scope, SharingStarted.Eagerly, BuildConfig.PRO_UNLOCKED)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        // If Play never answers (no Play Store, or a stuck one) don't leave the paywall spinning
        // forever: report billing as unavailable. A later answer from Play still wins.
        scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (_status.value == StoreStatus.Connecting) _status.value = StoreStatus.Unavailable
        }
    }

    fun start() {
        if (client.isReady) {
            _status.value = StoreStatus.Ready
            refresh()
            return
        }
        if (client.connectionState == BillingClient.ConnectionState.CONNECTING) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _status.value = StoreStatus.Ready
                    refresh()
                } else if (!client.isReady) {
                    Log.i(TAG, "Billing unavailable: ${result.debugMessage}")
                    _status.value = StoreStatus.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() = Unit // auto-reconnects on the next call
        })
    }

    /** Re-reads products and the user's purchases. Cheap; called whenever the app comes forward. */
    fun refresh() {
        if (!client.isReady) {
            start()
            return
        }
        queryPurchases()
        if (_plans.value.isEmpty()) queryPlans()
    }

    fun buy(activity: Activity, plan: ProPlan) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(plan.details)
                        .setOfferToken(plan.offerToken)
                        .build()
                )
            )
            .build()
        _busy.value = true
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode != BillingClient.BillingResponseCode.OK) _busy.value = false
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _busy.value = false
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handle(purchases)
        } else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryPurchases()
        }
    }

    private fun queryPurchases() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases)
        }
    }

    private fun handle(purchases: List<Purchase>) {
        val active = purchases.filter {
            PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        active.filterNot { it.isAcknowledged }.forEach { p ->
            // Unacknowledged purchases are refunded by Play after three days.
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            ) { r -> if (r.responseCode != BillingClient.BillingResponseCode.OK) Log.w(TAG, "ack failed: ${r.debugMessage}") }
        }
        val pro = active.isNotEmpty()
        entitled.value = pro
        scope.launch { settings.setProCached(pro) }
    }

    private fun queryPlans() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Products unavailable: ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            _plans.value = details.productDetailsList.flatMap(::plansOf).sortedByDescending { it.yearly }
        }
    }

    /** The best offer per base plan: Play only returns offers this user is eligible for. */
    private fun plansOf(d: ProductDetails): List<ProPlan> =
        d.subscriptionOfferDetails.orEmpty()
            .groupBy { it.basePlanId }
            .mapNotNull { (basePlan, offers) ->
                val withTrial = offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
                val offer = withTrial ?: offers.firstOrNull { it.offerId == null } ?: offers.first()
                val phases = offer.pricingPhases.pricingPhaseList
                val recurring = phases.lastOrNull() ?: return@mapNotNull null
                val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
                ProPlan(
                    details = d,
                    basePlanId = basePlan,
                    offerToken = offer.offerToken,
                    price = recurring.formattedPrice,
                    priceMicros = recurring.priceAmountMicros,
                    currency = recurring.priceCurrencyCode,
                    period = recurring.billingPeriod,
                    trialDays = trial?.let { periodDays(it.billingPeriod) * it.billingCycleCount.coerceAtLeast(1) },
                )
            }

    companion object {
        private const val TAG = "ProStore"
        const val PRODUCT_ID = "peekaboo_pro"
        private const val CONNECT_TIMEOUT_MS = 8_000L

        fun manageUrl(packageName: String) =
            "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=$packageName"

        /** Days in a simple ISO-8601 period such as P3D, P1W, P1M or P1Y. */
        fun periodDays(iso: String): Int {
            val m = Regex("""P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)W)?(?:(\d+)D)?""").matchEntire(iso) ?: return 0
            val (y, mo, w, d) = m.destructured
            return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
        }
    }
}
