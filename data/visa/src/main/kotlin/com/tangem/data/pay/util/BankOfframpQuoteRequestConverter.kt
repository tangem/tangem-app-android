package com.tangem.data.pay.util

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.spend.datasource.pay.models.request.BankOfframpQuoteRequest
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode

internal object BankOfframpQuoteRequestConverter :
    Converter<BankOfframpQuoteRequestConverter.Input, Either<BankOfframpError, BankOfframpQuoteRequest>> {

    private const val MAX_AMOUNT_SCALE = 2

    data class Input(
        val productInstanceId: String,
        val amount: BigDecimal,
        val recipient: BankOfframpRecipient,
    )

    private fun isQuotableAmount(amount: BigDecimal): Boolean =
        amount.signum() > 0 && amount.stripTrailingZeros().scale() <= MAX_AMOUNT_SCALE

    override fun convert(value: Input): Either<BankOfframpError, BankOfframpQuoteRequest> = either {
        ensure(isQuotableAmount(value.amount)) { BankOfframpError.Validation }
        val amount = value.amount.setScale(MAX_AMOUNT_SCALE, RoundingMode.UNNECESSARY)
        when (val recipient = value.recipient) {
            is BankOfframpRecipient.Saved -> BankOfframpQuoteRequest(
                productInstanceId = value.productInstanceId,
                amount = amount,
                bankAccountId = recipient.bankAccountId,
            )
            is BankOfframpRecipient.New -> BankOfframpQuoteRequest(
                productInstanceId = value.productInstanceId,
                amount = amount,
                bankAccount = BankOfframpOrderRequestConverter.convertBankAccount(
                    details = recipient.details,
                    rail = BankRail.SPEI,
                ).bind(),
            )
        }
    }
}