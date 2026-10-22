package com.tangem.data.transaction.convertes

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.data.transaction.api.models.SendIntentResponse
import com.tangem.data.transaction.api.models.SendIntentStatus
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class SendIntentConverter @Inject constructor() :
    Converter<SendIntentResponse, Either<SendIntentError, SendIntent>> {

    override fun convert(value: SendIntentResponse): Either<SendIntentError, SendIntent> = either {
        val amount = value.amount.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        ensureNotNull(amount) { SendIntentError.MalformedAmount(intentId = value.intentId, amount = value.amount) }

        SendIntent(
            id = value.intentId,
            clientId = value.clientId,
            sessionId = value.sessionId,
            isHandled = value.status != SendIntentStatus.CREATED || !value.txHash.isNullOrBlank(),
            networkId = value.networkId,
            tokenId = value.tokenId,
            fromAddress = value.fromAddress,
            toAddress = value.toAddress,
            amount = amount,
            memo = value.memo?.takeIf(String::isNotBlank),
            comment = value.comment?.takeIf(String::isNotBlank),
        )
    }
}