package com.tangem.features.yield.supply.impl.incentives.claim.model

import arrow.core.getOrElse
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.domain.yield.supply.usecase.GetFreshYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.ValidateYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.YieldClaimValidationContext
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

internal class YieldSupplyClaimPreparer @Inject constructor(
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
    private val getFreshYieldClaimCalldataUseCase: GetFreshYieldClaimCalldataUseCase,
    private val validateYieldClaimCalldataUseCase: ValidateYieldClaimCalldataUseCase,
) {

    suspend fun loadRewards(chainId: Int, userAddress: String): YieldSupplyClaimRewards? {
        return loadSummary(chainId = chainId, userAddress = userAddress)?.toClaimRewards()
    }

    /** Unlike [loadRewards], leaves out a deferred claim: only the owner may choose to claim against the advice. */
    suspend fun loadRecommendedRewards(chainId: Int, userAddress: String): YieldSupplyClaimRewards? {
        return (loadSummary(chainId = chainId, userAddress = userAddress) as? YieldRewardsSummary.Claimable)
            ?.toClaimRewards()
    }

    private suspend fun loadSummary(chainId: Int, userAddress: String): YieldRewardsSummary? {
        return getYieldRewardsSummaryUseCase(chainId = chainId, userAddress = userAddress)
            .getOrElse { error ->
                TangemLogger.e("Yield claim rewards unavailable: $error")
                null
            }
    }

    suspend fun loadValidatedCalldata(
        userWalletId: UserWalletId,
        network: Network,
        chainId: Int,
        userAddress: String,
        rewards: YieldSupplyClaimRewards,
    ): YieldClaimCalldata? {
        val calldata = getFreshYieldClaimCalldataUseCase(
            chainId = chainId,
            userAddress = userAddress,
        ).getOrElse { error ->
            TangemLogger.e("Yield claim calldata unavailable: $error")
            return null
        }

        return validateYieldClaimCalldataUseCase(
            calldata = calldata,
            context = YieldClaimValidationContext(
                userWalletId = userWalletId,
                network = network,
                chainId = chainId,
                userAddress = userAddress,
                moduleAddress = rewards.moduleAddress,
                supportedRewardTokens = rewards.supportedRewardTokens,
            ),
        ).getOrElse { error ->
            TangemLogger.e("Yield claim rejected before signing: $error")
            null
        }
    }

    companion object {
        /** Shorter than the Merkl proof rollover; bound to the proof, not to the fee the owner already accepted. */
        val PREPARATION_VALIDITY = 2.minutes
    }
}