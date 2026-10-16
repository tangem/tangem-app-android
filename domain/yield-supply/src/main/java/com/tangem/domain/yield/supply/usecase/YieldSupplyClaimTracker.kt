package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.normalizeEvmAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
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
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigInteger

/**
 * Polls on the app scope because the claim sheet is gone before the transaction is mined. A timeout publishes nothing:
 * the transaction may still land.
 *
 * A confirmed claim stays in flight until Merkl reflects it, since until then the backend still offers the claimed
 * rewards. A claim takes every reward of the module, so an in-flight claim is keyed by the module, not by the token.
 */
class YieldSupplyClaimTracker(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    private val yieldIncentivesRepository: YieldIncentivesRepository,
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
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

    /**
     * A second claim for a module already in flight is ignored.
     *
     * @param claimedRewards rewards the signed claim pays out; the claim counts as reflected once each of them has less
     * left to claim
     */
    @Suppress("LongParameterList")
    fun track(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        chainId: Int,
        userAddress: String,
        transactionHash: String,
        claimedRewards: List<YieldReward>,
    ) {
        val key = trackedKey(chainId = chainId, userAddress = userAddress)
        if (key in trackedKeys.getAndUpdate { tracked -> tracked + key }) return

        val claim = TrackedClaim(
            userWalletId = userWalletId,
            token = cryptoCurrency,
            chainId = chainId,
            userAddress = userAddress,
            transactionHash = transactionHash,
            claimedRewards = claimedRewards,
        )
        coroutineScope.launch {
            // A revert is released before publishing, so listeners see the claim settled when its outcome arrives
            val isReverted = try {
                withTimeoutOrNull(SETTLEMENT_TIMEOUT_MS) { isReverted(claim) } == true
            } finally {
                trackedKeys.update { it - key }
            }

            if (isReverted) outcomes.emit(claim.toOutcome(YieldSupplyTransactionOutcome.Reverted))
        }
    }

    private suspend fun isReverted(claim: TrackedClaim): Boolean {
        val outcome = awaitOutcome(claim)
        if (outcome == YieldSupplyTransactionOutcome.Reverted) return true

        if (outcome == YieldSupplyTransactionOutcome.Confirmed) outcomes.emit(claim.toOutcome(outcome))
        awaitReflected(claim)

        return false
    }

    private fun trackedKey(chainId: Int, userAddress: String) =
        TrackedKey(chainId = chainId, userAddress = userAddress.normalizeEvmAddress())

    private suspend fun awaitOutcome(claim: TrackedClaim): YieldSupplyTransactionOutcome? {
        repeat(MAX_ATTEMPTS) {
            delay(CHECK_INTERVAL_MS)

            val outcome = yieldSupplyTransactionRepository.getTransactionOutcome(
                userWalletId = claim.userWalletId,
                cryptoCurrency = claim.token,
                transactionHash = claim.transactionHash,
            )
            if (outcome != null) return outcome
        }

        return null
    }

    private suspend fun awaitReflected(claim: TrackedClaim) {
        while (!isReflected(claim)) delay(REFLECTION_CHECK_INTERVAL_MS)
    }

    private suspend fun isReflected(claim: TrackedClaim): Boolean {
        yieldIncentivesRepository.invalidateUserCache(chainId = claim.chainId, userAddress = claim.userAddress)
        val summary = getYieldRewardsSummaryUseCase(chainId = claim.chainId, userAddress = claim.userAddress)
            .getOrNull()
            ?: return false
        // A deferred summary can still list the claimed amounts: losing the recommendation does not mean Merkl
        // processed the claim
        val claimable = when (summary) {
            is YieldRewardsSummary.Claimable -> summary.claimable
            is YieldRewardsSummary.Deferred -> summary.claimable
            YieldRewardsSummary.None,
            is YieldRewardsSummary.PendingOnly,
            -> return true
        }

        return claim.claimedRewards.all { claimed ->
            val remainingAmount = claimable
                .firstOrNull { reward ->
                    reward.rewardToken.normalizeEvmAddress() == claimed.rewardToken.normalizeEvmAddress()
                }
                ?.claimableAmount
                ?: BigInteger.ZERO

            remainingAmount < claimed.claimableAmount
        }
    }

    private data class TrackedClaim(
        val userWalletId: UserWalletId,
        val token: CryptoCurrency.Token,
        val chainId: Int,
        val userAddress: String,
        val transactionHash: String,
        val claimedRewards: List<YieldReward>,
    ) {

        fun toOutcome(outcome: YieldSupplyTransactionOutcome) = YieldSupplyClaimOutcome(
            userWalletId = userWalletId,
            cryptoCurrencyId = token.id,
            chainId = chainId,
            userAddress = userAddress,
            outcome = outcome,
        )
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 10_000L

        /** Five minutes: long enough for a congested EVM block, short enough not to poll a dropped transaction. */
        const val MAX_ATTEMPTS = 30
        const val REFLECTION_CHECK_INTERVAL_MS = 15_000L

        /** Six minutes from the submission, mining included; Merkl reflects a claim within a few minutes. */
        const val SETTLEMENT_TIMEOUT_MS = 360_000L
        const val OUTCOME_BUFFER = 8
    }
}