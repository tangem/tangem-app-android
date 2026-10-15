package com.tangem.domain.yield.supply.usecase

import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal

/** Outcome of the pre-claim attempt made when the user withdraws from Yield Mode. */
sealed interface YieldPreClaim {

    /** There is nothing to claim, or the claim could not be prepared — the withdrawal goes on alone. */
    data object Skip : YieldPreClaim

    /** The claim the owner may sign before the withdrawal, with the context its outcome is followed by. */
    data class Ready(
        val transactionData: TransactionData.Uncompiled,
        val chainId: Int,
        val userAddress: String,
        val token: CryptoCurrency.Token,
    ) : YieldPreClaim
}

/**
 * Builds the rewards claim that precedes a Yield Mode withdrawal.
 *
 * Everything here is optional work on the way to the withdrawal, so every failure — an absent feature, an
 * unreachable backend, a payload the gate refuses, a fee that cannot be estimated — ends in [YieldPreClaim.Skip]
 * rather than an error the caller has to handle. Claiming is also skipped unless the owner can pay for both
 * transactions: the claim spends native gas that the withdrawal is about to need, and leaving the withdrawal
 * unaffordable would be the one way a pre-claim could cancel it.
 */
@Suppress("LongParameterList")
class PrepareClaimBeforeWithdrawUseCase(
    private val yieldSupplyMarketFlowUseCase: YieldSupplyMarketFlowUseCase,
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
    private val getFreshYieldClaimCalldataUseCase: GetFreshYieldClaimCalldataUseCase,
    private val validateYieldClaimCalldataUseCase: ValidateYieldClaimCalldataUseCase,
    private val buildYieldClaimTransactionUseCase: BuildYieldClaimTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        withdrawalFee: BigDecimal,
    ): YieldPreClaim {
        val token = cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: return skip("the asset is not a token")
        // The caller falls back to the token's own status when the fee-paying currency cannot be resolved, and
        // weighing native fees against a token balance would wave through a claim there is no gas for.
        if (feeCryptoCurrencyStatus.currency !is CryptoCurrency.Coin) {
            return skip("the native balance paying for both transactions is unknown")
        }
        val userAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: return skip("the owner address is unknown")
        val chainId = resolveChainId(token) ?: return skip("the token has no markets entry")

        // A claim the user started from the sheet is still being followed, here or on another token of the same
        // module: the same cumulative proof signed twice pays out nothing and costs gas the withdrawal is about
        // to need.
        if (yieldSupplyClaimTracker.isClaimInFlight(chainId = chainId, userAddress = userAddress).first()) {
            return skip("another claim on this module is still being followed")
        }

        val summary = getYieldRewardsSummaryUseCase(chainId = chainId, userAddress = userAddress)
            .getOrNull() as? YieldRewardsSummary.Claimable
            ?: return skip("there is no recommended reward to claim")

        val calldata = getFreshYieldClaimCalldataUseCase(chainId = chainId, userAddress = userAddress)
            .getOrNull()
            ?.let { fresh ->
                validateYieldClaimCalldataUseCase(
                    calldata = fresh,
                    context = YieldClaimValidationContext(
                        userWalletId = userWallet.walletId,
                        network = token.network,
                        chainId = chainId,
                        userAddress = userAddress,
                        moduleAddress = summary.moduleAddress,
                        supportedRewardTokens = summary.supportedRewardTokens,
                    ),
                ).getOrNull()
            }
            ?: return skip("the fresh calldata is missing or was rejected by the gate")

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
        if (claimFee + withdrawalFee > feeCryptoCurrencyStatus.value.amount.orZero()) {
            return skip("the owner cannot pay for both transactions")
        }

        return YieldPreClaim.Ready(
            transactionData = transactionData.copy(fee = fee),
            chainId = chainId,
            userAddress = userAddress,
            token = token,
        )
    }

    /** Logged, never raised: the caller has no decision to make here, but a claim that never happens is a report. */
    private fun skip(reason: String): YieldPreClaim {
        TangemLogger.i("Claim before withdrawal skipped: $reason")

        return YieldPreClaim.Skip
    }

    /**
     * Bounded on purpose: the markets cache is warm by the time a withdrawal starts, and a cold one must not hold
     * the withdrawal back — the claim is what waits for the cache, never the principal. A cache that cannot be read
     * at all is treated the same way.
     */
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