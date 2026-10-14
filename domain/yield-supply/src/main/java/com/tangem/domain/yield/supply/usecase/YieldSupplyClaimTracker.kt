package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.normalizeEvmAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Polls on the app scope because the claim sheet is gone before the transaction is mined. A timeout publishes nothing:
 * the transaction may still land.
 *
 * A claim takes every reward of the module, so an in-flight claim is keyed by the module, not by the token.
 */
class YieldSupplyClaimTracker(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    private val yieldIncentivesRepository: YieldIncentivesRepository,
    private val coroutineScope: AppCoroutineScope,
) {

    private data class TrackedKey(val chainId: Int, val userAddress: String)

    private val trackedKeys = MutableStateFlow<Set<TrackedKey>>(emptySet())

    /** Terminal outcomes only; a listener that subscribes late misses them. */
    val outcomes: SharedFlow<YieldSupplyClaimOutcome>
        field = MutableSharedFlow<YieldSupplyClaimOutcome>(extraBufferCapacity = OUTCOME_BUFFER)

    fun isClaimInFlight(chainId: Int, userAddress: String): Flow<Boolean> {
        val key = trackedKey(chainId = chainId, userAddress = userAddress)

        return trackedKeys.map { key in it }.distinctUntilChanged()
    }

    /** A second claim for a module already in flight is ignored. */
    fun track(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        chainId: Int,
        userAddress: String,
        transactionHash: String,
    ) {
        val key = trackedKey(chainId = chainId, userAddress = userAddress)
        if (key in trackedKeys.getAndUpdate { tracked -> tracked + key }) return

        coroutineScope.launch {
            // Released before publishing, so listeners see the claim settled when its outcome arrives
            val outcome = try {
                awaitOutcome(userWalletId = userWalletId, token = cryptoCurrency, hash = transactionHash)
                    ?.also { outcome -> invalidateIfConfirmed(outcome, chainId = chainId, userAddress = userAddress) }
            } finally {
                trackedKeys.update { it - key }
            } ?: return@launch

            outcomes.emit(
                YieldSupplyClaimOutcome(
                    userWalletId = userWalletId,
                    cryptoCurrencyId = cryptoCurrency.id,
                    chainId = chainId,
                    userAddress = userAddress,
                    outcome = outcome,
                ),
            )
        }
    }

    private fun trackedKey(chainId: Int, userAddress: String) =
        TrackedKey(chainId = chainId, userAddress = userAddress.normalizeEvmAddress())

    private suspend fun invalidateIfConfirmed(
        outcome: YieldSupplyTransactionOutcome,
        chainId: Int,
        userAddress: String,
    ) {
        if (outcome == YieldSupplyTransactionOutcome.Confirmed) {
            yieldIncentivesRepository.invalidateUserCache(chainId = chainId, userAddress = userAddress)
        }
    }

    private suspend fun awaitOutcome(
        userWalletId: UserWalletId,
        token: CryptoCurrency.Token,
        hash: String,
    ): YieldSupplyTransactionOutcome? {
        repeat(MAX_ATTEMPTS) {
            delay(CHECK_INTERVAL_MS)

            val outcome = yieldSupplyTransactionRepository.getTransactionOutcome(
                userWalletId = userWalletId,
                cryptoCurrency = token,
                transactionHash = hash,
            )
            if (outcome != null) return outcome
        }

        return null
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 10_000L

        /** Five minutes: long enough for a congested EVM block, short enough not to poll a dropped transaction. */
        const val MAX_ATTEMPTS = 30
        const val OUTCOME_BUFFER = 8
    }
}