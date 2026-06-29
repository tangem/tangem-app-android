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
        val required = feeAmount + sendAmountInFeeToken
        if (!isYieldActive) {
            return@either if (totalBalance >= required) {
                GaslessFeePlan.TokenPay(feeToken = token, fee = tokenFee)
            } else {
                raise(GaslessError.NotEnoughFunds)
            }
        }

        val moduleBalance = gaslessYieldRepository
            .getEffectiveProtocolBalance(userWallet.walletId, token) ?: BigDecimal.ZERO

        // Liquid balance already on the EOA = total - what is held inside the yield module.
        val liquidBalance = (totalBalance - moduleBalance).coerceAtLeast(BigDecimal.ZERO)
        if (liquidBalance >= required) {
            return@either GaslessFeePlan.TokenPay(feeToken = token, fee = tokenFee)
        }

        if (totalBalance < required) raise(GaslessError.NotEnoughFunds)

        val liquidLeftForFee = (liquidBalance - sendAmountInFeeToken).coerceAtLeast(BigDecimal.ZERO)
        val withdrawAmountDecimal = (feeAmount - liquidLeftForFee).coerceAtLeast(BigDecimal.ZERO)

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
}