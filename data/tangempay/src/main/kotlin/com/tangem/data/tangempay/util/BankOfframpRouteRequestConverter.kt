package com.tangem.data.tangempay.util

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.spend.datasource.tangempay.models.request.BankOfframpRouteRequest
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