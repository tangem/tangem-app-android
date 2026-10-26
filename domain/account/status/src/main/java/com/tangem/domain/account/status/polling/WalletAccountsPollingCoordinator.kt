package com.tangem.domain.account.status.polling

import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Polls a wallet's accounts document every [POLL_INTERVAL] while at least one subscriber collects [poll], so tokens
 * another joint-account member adds show up without a pull-to-refresh. Paced like
 * [com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator]: one ticker per wallet, the first fetch
 * waits a full interval (the screen already fetched on open), and [lastFetchAt] outlives a torn-down ticker.
 *
 * A currency that arrives this way has no balance yet — nothing else asks for one — so each tick diffs the wallet's
 * currencies against the previous tick and fetches balances for the added ones. Diffing against the previous tick
 * rather than within one also catches a supplier that reflects the stored document a tick late.
 */
class WalletAccountsPollingCoordinator(
    private val fetcher: SingleAccountListFetcher,
    private val currenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val balanceFetcher: CryptoCurrencyBalanceFetcher,
    private val appScope: AppCoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val tickers = ConcurrentHashMap<UserWalletId, Flow<Unit>>()
    private val lastFetchAt = ConcurrentHashMap<UserWalletId, Long>()
    private val knownCurrencies = ConcurrentHashMap<UserWalletId, Set<CryptoCurrency>>()

    fun poll(walletId: UserWalletId): Flow<Unit> = tickers.computeIfAbsent(walletId, ::createTicker)

    private fun createTicker(walletId: UserWalletId): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            awaitNextFetch(walletId)
            tick(walletId)
            emit(Unit)
        }
    }.retryWhen { cause, _ ->
        TangemLogger.e("Wallet accounts polling ticker crashed for wallet $walletId, retrying", cause)
        delay(POLL_INTERVAL)
        true
    }.shareIn(
        scope = appScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
        replay = 0,
    )

    private suspend fun awaitNextFetch(walletId: UserWalletId) {
        val elapsedSinceLastFetch = lastFetchAt[walletId]?.let { now() - it } ?: 0L
        val remaining = POLL_INTERVAL.inWholeMilliseconds - elapsedSinceLastFetch
        if (remaining > 0) delay(remaining.milliseconds)
    }

    private suspend fun tick(walletId: UserWalletId) {
        val before = knownCurrencies[walletId] ?: currencies(walletId)
        val result = fetcher(SingleAccountListFetcher.Params(userWalletId = walletId))
        lastFetchAt[walletId] = now()

        if (result.isLeft()) {
            result.onLeft { TangemLogger.e("Failed to poll the accounts document for wallet $walletId", it) }
            knownCurrencies[walletId] = before
            return
        }

        val after = currencies(walletId)
        knownCurrencies[walletId] = after
        val added = (after - before).toList()
        if (added.isNotEmpty()) balanceFetcher(userWalletId = walletId, currencies = added)
    }

    private suspend fun currencies(walletId: UserWalletId): Set<CryptoCurrency> {
        val params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = walletId)
        return currenciesSupplier(params).firstOrNull().orEmpty()
    }

    private companion object {
        val POLL_INTERVAL = 10.seconds
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}