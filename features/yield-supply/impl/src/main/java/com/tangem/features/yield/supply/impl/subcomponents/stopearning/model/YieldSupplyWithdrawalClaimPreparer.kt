package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.usecase.BuildYieldClaimTransactionUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimPreparer
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import javax.inject.Inject

internal sealed interface YieldPreClaim {

    data object Skip : YieldPreClaim

    data class Ready(
        val transactionData: TransactionData.Uncompiled,
        val chainId: Int,
        val userAddress: String,
        val token: CryptoCurrency.Token,
        val claimedRewards: List<YieldReward>,
    ) : YieldPreClaim
}

/**
 * Every failure ends in [YieldPreClaim.Skip]: the claim is optional and must never block the withdrawal. It is also
 * skipped unless the fee-paying balance covers both transactions, so it cannot leave the withdrawal unaffordable.
 */
internal class YieldSupplyWithdrawalClaimPreparer @Inject constructor(
    private val yieldSupplyMarketFlowUseCase: YieldSupplyMarketFlowUseCase,
    private val yieldSupplyClaimPreparer: YieldSupplyClaimPreparer,
    private val buildYieldClaimTransactionUseCase: BuildYieldClaimTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
) {

    suspend fun prepare(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        withdrawalFee: BigDecimal,
    ): YieldPreClaim {
        val token = cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: return skip("the asset is not a token")
        val userAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: return skip("the owner address is unknown")
        val chainId = resolveChainId(token) ?: return skip("the token has no markets entry")

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

        val claimFee = fee.amount.value ?: return skip("the claim fee amount is unknown")
        // The caller falls back to the token's own status when the fee currency cannot be resolved
        if (!fee.amount.type.isPaidFrom(currency = feeCryptoCurrencyStatus.currency, network = token.network)) {
            return skip("the claim fee is not paid from the balance it is weighed against")
        }
        if (claimFee + withdrawalFee > feeCryptoCurrencyStatus.value.amount.orZero()) {
            return skip("the owner cannot pay for both transactions")
        }

        return YieldPreClaim.Ready(
            transactionData = transactionData.copy(fee = fee),
            chainId = chainId,
            userAddress = userAddress,
            token = token,
            claimedRewards = rewards.claimable,
        )
    }

    private fun skip(reason: String): YieldPreClaim {
        TangemLogger.i("Claim before withdrawal skipped: $reason")

        return YieldPreClaim.Skip
    }

    private fun AmountType.isPaidFrom(currency: CryptoCurrency, network: Network): Boolean {
        if (currency.network != network) return false

        return when (this) {
            AmountType.Coin -> currency is CryptoCurrency.Coin
            is AmountType.Token -> currency.isTokenOf(token.contractAddress)
            is AmountType.TokenYieldSupply -> currency.isTokenOf(token.contractAddress)
            // Neither is a balance the two fees can be summed against
            is AmountType.FeeResource, AmountType.Reserve -> false
        }
    }

    private fun CryptoCurrency.isTokenOf(contractAddress: String): Boolean {
        return this is CryptoCurrency.Token && this.contractAddress.equals(contractAddress, ignoreCase = true)
    }

    /** Bounded so a cold or unreadable markets cache never delays the withdrawal. */
    private suspend fun resolveChainId(token: CryptoCurrency.Token): Int? {
        return runSuspendCatching {
            withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
                yieldSupplyMarketFlowUseCase(token).mapNotNull { market -> market?.chainId }.firstOrNull()
            }
        }
            .onFailure { TangemLogger.e("Claim before withdrawal: the markets cache could not be read", it) }
            .getOrNull()
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MILLIS = 2_000L
    }
}