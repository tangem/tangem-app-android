package com.tangem.features.yield.supply.impl.incentives.claim.model

import arrow.core.getOrElse
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.usecase.GetFreshYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.ValidateYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.YieldClaimValidationContext
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class YieldSupplyClaimPreparer @Inject constructor(
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
    private val getFreshYieldClaimCalldataUseCase: GetFreshYieldClaimCalldataUseCase,
    private val validateYieldClaimCalldataUseCase: ValidateYieldClaimCalldataUseCase,
) {

    suspend fun loadRewards(chainId: Int, userAddress: String): YieldSupplyClaimRewards? {
        return getYieldRewardsSummaryUseCase(chainId = chainId, userAddress = userAddress)
            .getOrElse { error ->
                TangemLogger.e("Yield claim sheet has no rewards to show: $error")
                return null
            }
            .toClaimRewards()
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
}