package com.tangem.features.send.send.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.api.subcomponents.feeSelector.utils.FeeCalculationUtils.checkAndCalculateSubtractedAmount
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import javax.inject.Inject

internal class TronGaslessSentAmountQuoter @Inject constructor(
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
) {

    @Suppress("LongParameterList")
    suspend fun quoteForSentAmount(
        fee: TransactionFeeExtended,
        userWalletId: UserWalletId,
        sentCurrencyStatus: CryptoCurrencyStatus,
        enteredAmount: BigDecimal,
        reduceAmountBy: BigDecimal,
        requote: suspend (sentAmount: BigDecimal) -> Either<GetFeeError, TransactionFeeExtended>,
    ): Either<GetFeeError, TransactionFeeExtended> {
        var current = fee
        repeat(MAX_REQUOTES) {
            val quote = current.tronGaslessQuote ?: return current.right()
            val sentAmount = sentAmount(
                fee = current,
                userWalletId = userWalletId,
                sentCurrencyStatus = sentCurrencyStatus,
                enteredAmount = enteredAmount,
                reduceAmountBy = reduceAmountBy,
            )
            val sentAmountRaw = sentAmount.movePointRight(sentCurrencyStatus.currency.decimals).toBigInteger()
            if (sentAmountRaw == quote.quotedAmountRaw) return current.right()

            current = requote(sentAmount).getOrElse { error ->
                TangemLogger.e("Tron gasless re-quote for the reduced amount failed ($error), keeping the last quote")
                return current.right()
            }
        }
        return current.right()
    }

    private suspend fun sentAmount(
        fee: TransactionFeeExtended,
        userWalletId: UserWalletId,
        sentCurrencyStatus: CryptoCurrencyStatus,
        enteredAmount: BigDecimal,
        reduceAmountBy: BigDecimal,
    ): BigDecimal {
        val isAmountSubtractAvailable = isAmountSubtractAvailableUseCase(
            userWalletId = userWalletId,
            currency = sentCurrencyStatus.currency,
            maybeGaslessFee = fee.feeTokenId to fee.transactionFee.normal,
        ).getOrElse { false }

        return checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = sentCurrencyStatus,
            amountValue = enteredAmount,
            feeValue = fee.transactionFee.normal.amount.value,
            reduceAmountBy = reduceAmountBy,
        )
    }

    private companion object {
        const val MAX_REQUOTES = 3
    }
}