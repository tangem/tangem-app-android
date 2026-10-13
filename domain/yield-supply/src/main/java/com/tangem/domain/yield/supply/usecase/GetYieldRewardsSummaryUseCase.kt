package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.incentives.YieldIncentives
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal
import java.math.BigInteger

/** Merkl rewards of a user's yield module on a chain, reduced to the state the reward surfaces act on. */
class GetYieldRewardsSummaryUseCase(
    private val yieldIncentivesRepository: YieldIncentivesRepository,
) {

    suspend operator fun invoke(chainId: Int, userAddress: String): Either<YieldIncentivesError, YieldRewardsSummary> =
        yieldIncentivesRepository
            .getIncentives(chainId = chainId, userAddress = userAddress)
            .map(::summarise)

    private fun summarise(incentives: YieldIncentives): YieldRewardsSummary {
        val moduleAddress = incentives.moduleAddress?.takeIf(String::isNotBlank) ?: return YieldRewardsSummary.None
        val blockchain = Blockchain.fromChainId(incentives.chainId) ?: return YieldRewardsSummary.None
        val isCaseInsensitive = BlockchainUtils.isCaseInsensitiveContractAddress(blockchain.toNetworkId())

        // A claim pays out a token's whole cumulative amount, so one unroutable row for a token makes every row of
        // that token unroutable: conflicting rows fail closed instead of admitting the payout.
        val unroutableTokens = incentives.rewards
            .filterNot { it.isRoutableOn(blockchain) }
            .map(YieldReward::rewardToken)
        val (supported, unsupported) = incentives.rewards.partition { reward ->
            unroutableTokens.none { token -> token.equals(reward.rewardToken, ignoreCase = isCaseInsensitive) }
        }

        val claimable = supported.filter { it.claimableAmount > BigInteger.ZERO }
        val pending = supported.filter { it.pendingAmount > BigInteger.ZERO }
        // Amounts deliberately left out: a cumulative proof keeps listing a token long after its claimable amount
        // is gone, and admitting a claim is about what may be routed, not about what is owed right now.
        val supportedRewardTokens = supported.mapTo(mutableSetOf(), YieldReward::rewardToken)

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

    private fun List<YieldReward>.totalNetUsd(): BigDecimal =
        fold(BigDecimal.ZERO) { total, reward -> total + reward.estimatedNetValueUsd }

    /**
     * A reward without a valid token address cannot be told apart from another such reward, so letting it into the
     * allow-list would admit any claim naming the same malformed token; it is treated as unroutable instead.
     */
    private fun YieldReward.isRoutableOn(blockchain: Blockchain): Boolean =
        finalDestination != YieldRewardDestination.Unsupported && blockchain.validateAddress(rewardToken)
}