package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.usecase.BuildYieldClaimTransactionUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.impl.incentives.claim.di.YieldClaimTimeSource
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimPreparer
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal sealed interface YieldPreClaim {

    data object Skip : YieldPreClaim

    data class Ready(
        val transactionData: TransactionData.Uncompiled,
        val chainId: Int,
        val userAddress: String,
        val token: CryptoCurrency.Token,
        val claimedRewards: List<YieldReward>,
        val preparedAt: TimeMark,
    ) : YieldPreClaim
}

/**
 * Every failure ends in [YieldPreClaim.Skip], so a claim that cannot be prepared leaves the withdrawal to go alone.
 * Whether the owner can pay for both transactions is weighed by the withdrawal sheet.
 */
internal class YieldSupplyWithdrawalClaimPreparer @Inject constructor(
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
    private val yieldSupplyClaimPreparer: YieldSupplyClaimPreparer,
    private val buildYieldClaimTransactionUseCase: BuildYieldClaimTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
    @YieldClaimTimeSource private val timeSource: TimeSource,
) {

    suspend fun prepare(userWallet: UserWallet, cryptoCurrencyStatus: CryptoCurrencyStatus): YieldPreClaim {
        val token = cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: return skip("the asset is not a token")
        val userAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: return skip("the owner address is unknown")
        val chainId = yieldSupplyGetTokenStatusUseCase(token).getOrNull()?.chainId
            ?: return skip("the token has no markets entry")

        // The same cumulative proof signed twice pays out nothing and burns gas the withdrawal needs
        if (yieldSupplyClaimTracker.isClaimInFlight(chainId = chainId, userAddress = userAddress).first()) {
            return skip("another claim on this module is still being followed")
        }

        val rewards = yieldSupplyClaimPreparer.loadRecommendedRewards(chainId = chainId, userAddress = userAddress)
            ?: return skip("there is no recommended reward to claim")

        val calldata = yieldSupplyClaimPreparer.loadValidatedCalldata(
            userWalletId = userWallet.walletId,
            network = token.network,
            chainId = chainId,
            userAddress = userAddress,
            rewards = rewards,
        ) ?: return skip("the fresh calldata is missing or was rejected by the gate")
        val preparedAt = timeSource.markNow()

        val transactionData = buildYieldClaimTransactionUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            calldata = calldata,
            fee = null,
        ).getOrNull() ?: return skip("the claim transaction could not be built")

        val fee = getFeeUseCase(
            userWallet = userWallet,
            network = token.network,
            transactionData = transactionData,
        ).getOrNull()?.normal ?: return skip("the claim fee could not be estimated")

        if (fee.amount.value == null) return skip("the claim fee amount is unknown")
        val feeAmountType = fee.amount.type
        // The sheet adds this fee to the withdrawal fee; a resource or a reserve cannot be summed that way
        if (feeAmountType is AmountType.FeeResource || feeAmountType == AmountType.Reserve) {
            return skip("the claim fee is not paid from a balance the two fees can be summed against")
        }

        return YieldPreClaim.Ready(
            transactionData = transactionData.copy(fee = fee),
            chainId = chainId,
            userAddress = userAddress,
            token = token,
            claimedRewards = rewards.claimable,
            preparedAt = preparedAt,
        )
    }

    private fun skip(reason: String): YieldPreClaim {
        TangemLogger.i("Claim before withdrawal skipped: $reason")

        return YieldPreClaim.Skip
    }
}