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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Follows a submitted Merkl rewards claim until the chain says what became of it.
 *
 * The sheet is gone by the time the transaction is mined, so the outcome is resolved on an application-wide scope
 * and published to whoever is still on screen. Nothing is published after [MAX_ATTEMPTS] checks: the transaction may
 * still land, and calling it a failure would be a worse answer than saying nothing.
 *
 * A claim names no token and takes every reward the module holds, so an in-flight claim is keyed by the module, not
 * by the token it was started from.
 *
 * Separate from [YieldSupplyPendingTracker] deliberately: that one owns the enter/exit protocol state and would
 * re-announce the module as active, which a claim must not do.
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

    /** Starts following [transactionHash]; a claim already being followed for the same module is left alone. */
    fun track(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        chainId: Int,
        userAddress: String,
        transactionHash: String,
    ) {
        val key = trackedKey(chainId = chainId, userAddress = userAddress)
        var isAdded = false
        trackedKeys.update { tracked ->
            isAdded = key !in tracked
            tracked + key
        }
        if (!isAdded) return

        coroutineScope.launch {
            // Released in a finally, so a claim that times out or is cancelled does not stay in flight for the rest
            // of the process.
            try {
                val outcome = awaitOutcome(userWalletId = userWalletId, token = cryptoCurrency, hash = transactionHash)
                    ?: return@launch

                if (outcome == YieldSupplyTransactionOutcome.Confirmed) {
                    yieldIncentivesRepository.invalidateUserCache(chainId = chainId, userAddress = userAddress)
                }

                trackedKeys.update { it - key }
                outcomes.emit(
                    YieldSupplyClaimOutcome(
                        userWalletId = userWalletId,
                        cryptoCurrencyId = cryptoCurrency.id,
                        chainId = chainId,
                        userAddress = userAddress,
                        outcome = outcome,
                    ),
                )
            } finally {
                trackedKeys.update { it - key }
            }
        }
    }

    private fun trackedKey(chainId: Int, userAddress: String) =
        TrackedKey(chainId = chainId, userAddress = userAddress.normalizeEvmAddress())

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