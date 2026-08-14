package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import java.math.RoundingMode

class ResolveGaslessFeePlanUseCase(
    private val gaslessYieldRepository: GaslessYieldRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        tokenStatus: CryptoCurrencyStatus,
        tokenFee: Fee.Ethereum.TokenCurrency,
        isYieldActive: Boolean,
        sendAmountInFeeToken: BigDecimal,
    ): Either<GetFeeError, GaslessFeePlan> = either {
        val token = tokenStatus.currency as? CryptoCurrency.Token
            ?: raise(GaslessError.DataError(IllegalStateException("fee currency must be a token")))

        val feeAmount = tokenFee.amount.value
            ?: raise(GaslessError.DataError(IllegalStateException("token fee amount is null")))
        val totalBalance = tokenStatus.value.amount ?: BigDecimal.ZERO
        val required = resolveRequiredBalance(
            sendAmountInFeeToken = sendAmountInFeeToken,
            feeAmount = feeAmount,
            totalBalance = totalBalance,
        )
        if (!isYieldActive) {
            val liquidBalance = (totalBalance - resolveModuleBalanceForInactiveYield(userWallet, tokenStatus, token))
                .coerceAtLeast(BigDecimal.ZERO)

            return@either if (liquidBalance >= required) {
                GaslessFeePlan.TokenPay(feeToken = token, fee = tokenFee)
            } else {
                raise(GaslessError.NotEnoughFunds)
            }
        }

        // An unreadable balance is not an empty module: degrading it to zero would make the whole effective
        // balance look liquid and yield a TokenPay plan that cannot be settled. Give up on the token fee
        // instead and let the caller fall back to the native one.
        val moduleBalance = runSuspendCatching {
            gaslessYieldRepository.getEffectiveProtocolBalance(userWallet.walletId, token)
        }
            .onFailure { logger.e("Failed to read the yield module balance of ${token.symbol}", it) }
            .getOrNull() ?: raise(GaslessError.YieldBalanceUnavailable)

        // Liquid balance already on the EOA = total - what is held inside the yield module.
        val liquidBalance = (totalBalance - moduleBalance).coerceAtLeast(BigDecimal.ZERO)
        if (liquidBalance >= required) {
            return@either GaslessFeePlan.TokenPay(feeToken = token, fee = tokenFee)
        }

        if (totalBalance < required) raise(GaslessError.NotEnoughFunds)

        val liquidLeftForFee = if (sendAmountInFeeToken.signum() == 0) liquidBalance else BigDecimal.ZERO
        val withdrawAmountDecimal = (feeAmount - liquidLeftForFee).coerceAtLeast(BigDecimal.ZERO)

        if (withdrawAmountDecimal > moduleBalance) raise(GaslessError.NotEnoughFunds)

        val withdrawCallData = catch(
            block = {
                gaslessYieldRepository.createPartialWithdrawCallData(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = token,
                    amount = Amount(
                        token = Token(token.symbol, token.contractAddress, token.decimals),
                        value = withdrawAmountDecimal,
                    ),
                )
            },
            catch = { error ->
                when (error) {
                    is YieldModuleUpgradeUnavailableException,
                    is YieldModuleVersionIndeterminateException,
                    -> raise(GaslessError.ModuleUpdateUnavailable)
                    else -> raise(GaslessError.DataError(error))
                }
            },
        )

        val yieldModuleAddress = gaslessYieldRepository
            .getYieldContractAddress(userWallet.walletId, token)
            ?: raise(GaslessError.DataError(IllegalStateException("yield module address is null")))

        GaslessFeePlan.TokenPayWithYieldWithdraw(
            feeToken = token,
            fee = tokenFee,
            withdrawAmount = withdrawAmountDecimal
                .movePointRight(token.decimals)
                .setScale(0, RoundingMode.CEILING)
                .toBigInteger(),
            withdrawCallData = withdrawCallData,
            yieldModuleAddress = yieldModuleAddress,
        )
    }

    private fun resolveRequiredBalance(
        sendAmountInFeeToken: BigDecimal,
        feeAmount: BigDecimal,
        totalBalance: BigDecimal,
    ): BigDecimal {
        val isReducedByFee = totalBalance < sendAmountInFeeToken + feeAmount &&
            totalBalance >= sendAmountInFeeToken &&
            feeAmount < sendAmountInFeeToken

        return if (isReducedByFee) totalBalance else sendAmountInFeeToken + feeAmount
    }

    private suspend fun resolveModuleBalanceForInactiveYield(
        userWallet: UserWallet,
        tokenStatus: CryptoCurrencyStatus,
        token: CryptoCurrency.Token,
    ): BigDecimal {
        val yieldSupplyStatus = tokenStatus.value.yieldSupplyStatus

        yieldSupplyStatus?.effectiveProtocolBalance?.let { return it }
        if (yieldSupplyStatus?.isInitialized == false) return BigDecimal.ZERO

        return runSuspendCatching {
            gaslessYieldRepository.getEffectiveProtocolBalance(userWallet.walletId, token)
        }
            .onFailure { logger.e("Failed to read the yield module balance of ${token.symbol}, assuming none", it) }
            .getOrNull() ?: BigDecimal.ZERO
    }

    private companion object {
        val logger = TangemLogger.withTag("ResolveGaslessFeePlanUseCase")
    }
}