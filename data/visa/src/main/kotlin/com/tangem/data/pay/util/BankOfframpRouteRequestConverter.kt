package com.tangem.data.pay.util

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.spend.datasource.pay.models.request.BankOfframpRouteRequest
import com.tangem.utils.converter.Converter

internal object BankOfframpRouteRequestConverter :
    Converter<BankOfframpRouteRequestConverter.Input, Either<BankOfframpError, BankOfframpRouteRequest>> {

    data class Input(
        val productInstanceId: String,
        val rail: BankRail,
        val recipient: BankOfframpRecipient,
    )

    override fun convert(value: Input): Either<BankOfframpError, BankOfframpRouteRequest> = either {
        when (val recipient = value.recipient) {
            is BankOfframpRecipient.Saved -> BankOfframpRouteRequest(
                productInstanceId = value.productInstanceId,
                bankAccountId = recipient.bankAccountId,
            )
            is BankOfframpRecipient.New -> BankOfframpRouteRequest(
                productInstanceId = value.productInstanceId,
                bankAccount = BankOfframpOrderRequestConverter.convertBankAccount(
                    details = recipient.details,
                    rail = value.rail,
                ).bind(),
            )
        }
    }
}