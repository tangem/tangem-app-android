package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.normalizeEvmAddress
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.incentives.YieldIncentives
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import java.math.BigDecimal
import java.math.BigInteger

/** Merkl rewards of a user's yield module on a chain, reduced to the state the reward surfaces act on. */
class GetYieldRewardsSummaryUseCase(
    private val yieldIncentivesRepository: YieldIncentivesRepository,
) {

    /**
     * A reward without a well-formed token address cannot be told apart from another such reward, so letting it into
     * the allow-list would admit any claim naming the same malformed token; it is treated as unroutable instead.
     */
    private val YieldReward.isSupported: Boolean
        get() = finalDestination != YieldRewardDestination.Unsupported && EVM_ADDRESS_FORMAT.matches(rewardToken)

    suspend operator fun invoke(chainId: Int, userAddress: String): Either<YieldIncentivesError, YieldRewardsSummary> =
        yieldIncentivesRepository
            .getIncentives(chainId = chainId, userAddress = userAddress)
            .map(::summarise)

    private fun summarise(incentives: YieldIncentives): YieldRewardsSummary {
        val moduleAddress = incentives.moduleAddress?.takeIf(String::isNotBlank) ?: return YieldRewardsSummary.None

        val claimable = incentives.rewards.filter(::isClaimable)
        val pending = incentives.rewards.filter(::hasPending)
        val unsupported = incentives.rewards.filterNot { it.isSupported }
        // Amounts deliberately left out: a cumulative proof keeps listing a token long after its claimable amount
        // is gone, and admitting a claim is about what may be routed, not about what is owed right now.
        val supportedRewardTokens = incentives.rewards
            .filter { it.isSupported }
            .mapTo(mutableSetOf()) { it.rewardToken.normalizeEvmAddress() }

        return when {
            claimable.isEmpty() && pending.isEmpty() -> YieldRewardsSummary.None
            claimable.isEmpty() -> YieldRewardsSummary.PendingOnly(pending = pending)
            incentives.isClaimRecommended -> YieldRewardsSummary.Claimable(
                moduleAddress = moduleAddress,
                claimable = claimable,
                pending = pending,
                unsupported = unsupported,
                supportedRewardTokens = supportedRewardTokens,
                totalNetUsd = claimable.totalNetUsd(),
            )
            else -> YieldRewardsSummary.Deferred(
                moduleAddress = moduleAddress,
                claimable = claimable,
                pending = pending,
                unsupported = unsupported,
                supportedRewardTokens = supportedRewardTokens,
                totalNetUsd = claimable.totalNetUsd(),
            )
        }
    }

    private fun isClaimable(reward: YieldReward): Boolean =
        reward.claimableAmount > BigInteger.ZERO && reward.isSupported

    private fun hasPending(reward: YieldReward): Boolean = reward.pendingAmount > BigInteger.ZERO && reward.isSupported

    private fun List<YieldReward>.totalNetUsd(): BigDecimal =
        fold(BigDecimal.ZERO) { total, reward -> total + reward.estimatedNetValueUsd }

    private companion object {
        val EVM_ADDRESS_FORMAT = Regex(pattern = "^0x[0-9a-fA-F]{40}$")
    }
}