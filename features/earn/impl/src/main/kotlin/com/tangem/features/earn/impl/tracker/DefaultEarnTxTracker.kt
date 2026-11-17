package com.tangem.features.earn.impl.tracker

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.earn.impl.analytics.EarnTxResultAnalyticsEvent
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxTracker
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@Singleton
internal class DefaultEarnTxTracker @Inject constructor(
    private val store: EarnTxStore,
    private val registry: EarnProductsRegistry,
    private val sessionFactories: Map<String, @JvmSuppressWildcards EarnSession.Factory>,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val accountListSupplier: SingleAccountListSupplier,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appScope: AppCoroutineScope,
    private val clock: EarnTxClock,
) : EarnTxTracker {

    private val mutex = Mutex()
    private val trackedIds = mutableSetOf<String>()

    override fun onSent(ref: EarnTxRef) {
        appScope.launch {
            store.add(ref)
            track(ref)
        }
    }

    override fun resume() {
        appScope.launch {
            store.all().forEach { ref ->
                launch { track(ref) }
            }
        }
    }

    private suspend fun track(ref: EarnTxRef) {
        if (!claim(ref.id)) return

        try {
            runSuspendCatching { follow(ref) }
                .onFailure { TangemLogger.e("Earn tracker: tracking of ${ref.productId.value} failed, kept", it) }
        } finally {
            withContext(NonCancellable) { release(ref.id) }
        }
    }

    private suspend fun follow(ref: EarnTxRef) {
        // A journal read made before another tracking resolved this record must not report it again
        if (store.all().none { it.id == ref.id }) return

        if (clock.now() - ref.createdAt > RECORD_TTL) {
            drop(ref, reason = "the result is still unknown after $RECORD_TTL")
            return
        }

        val product = registry.find(ref.productId)
        val factory = sessionFactories[ref.productId.value]
        if (product == null || factory == null) {
            drop(ref, reason = "the product or its session is unknown")
            return
        }

        val wallets = withTimeoutOrNull(CURRENCY_TIMEOUT) {
            userWalletsListRepository.userWallets.filterNotNull().first()
        }
        if (wallets == null) {
            keep(ref, reason = "the wallets are not loaded")
            return
        }
        // The account list of a removed wallet never emits, it is only retried, so a removed wallet is checked here
        if (wallets.none { it.walletId == ref.userWalletId }) {
            drop(ref, reason = "the wallet is removed")
            return
        }

        val accountList = withTimeoutOrNull(CURRENCY_TIMEOUT) { accountListSupplier(ref.userWalletId).first() }
        if (accountList == null) {
            keep(ref, reason = "the account list of the wallet is unavailable")
            return
        }
        val currency = accountList.getCryptoCurrency(ref.currencyId.value).getOrNull()
        if (currency == null) {
            drop(ref, reason = "the wallet has no such currency")
            return
        }

        val status = factory.create(EarnProduct.Params(userWalletId = ref.userWalletId, currency = currency))
            .track(ref)
            .map { it.status }
            .firstOrNull { it != TxInfo.TransactionStatus.Unconfirmed }

        report(ref = ref, product = product, status = status)
    }

    private suspend fun report(ref: EarnTxRef, product: EarnProduct, status: TxInfo.TransactionStatus?) {
        val event = when (status) {
            TxInfo.TransactionStatus.Confirmed -> EarnTxResultAnalyticsEvent.TransactionConfirmed(
                category = product.analytics.category,
                params = ref.analytics.params + ref.analytics.confirmedParams,
            )
            TxInfo.TransactionStatus.Failed -> EarnTxResultAnalyticsEvent.TransactionFailed(
                category = product.analytics.category,
                params = ref.analytics.params,
            )
            TxInfo.TransactionStatus.Unconfirmed, null -> {
                keep(ref, reason = "the result is unknown")
                return
            }
        }

        // Removed before sending: a crash in between loses the event instead of sending it twice
        store.remove(ref.id)
        analyticsEventHandler.send(event)
    }

    private suspend fun drop(ref: EarnTxRef, reason: String) {
        store.remove(ref.id)
        TangemLogger.e("Earn tracker: ${ref.productId.value} record dropped, $reason")
    }

    private fun keep(ref: EarnTxRef, reason: String) {
        TangemLogger.i("Earn tracker: ${ref.productId.value} record kept for the next start, $reason")
    }

    private suspend fun claim(id: String): Boolean = mutex.withLock { trackedIds.add(id) }

    private suspend fun release(id: String) {
        mutex.withLock { trackedIds.remove(id) }
    }

    private companion object {
        val CURRENCY_TIMEOUT = 30.seconds
        val RECORD_TTL = 7.days
    }
}