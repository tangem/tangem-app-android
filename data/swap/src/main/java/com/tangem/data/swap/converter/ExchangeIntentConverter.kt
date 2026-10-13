package com.tangem.data.swap.converter

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.ExchangeIntentError
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.grow.datasource.express.models.response.ExchangeIntentResponse
import com.tangem.grow.datasource.express.models.response.RateType
import com.tangem.utils.converter.Converter

/** Express sends the amount of one side only, so a response with both or neither amount is rejected. */
internal class ExchangeIntentConverter :
    Converter<ExchangeIntentResponse, Either<ExchangeIntentError, ExchangeIntent>> {

    override fun convert(value: ExchangeIntentResponse): Either<ExchangeIntentError, ExchangeIntent> = either {
        val intent = value.intent
        val fromAmount = intent.fromAmount
        val toAmount = intent.toAmount
        val (amountType, amount) = when {
            fromAmount != null && toAmount == null -> SwapAmountType.From to fromAmount
            toAmount != null && fromAmount == null -> SwapAmountType.To to toAmount
            else -> raise(ExchangeIntentError.AmbiguousAmountSide(intent.intentId))
        }

        ExchangeIntent(
            id = intent.intentId,
            walletId = intent.walletId,
            clientId = intent.clientId,
            sessionId = intent.sessionId,
            isHandled = value.transactions.isNotEmpty(),
            amountType = amountType,
            amount = amount,
            from = ExchangeIntent.Asset(
                network = intent.fromNetwork,
                contractAddress = intent.fromContractAddress,
                decimals = intent.fromDecimals,
                address = intent.fromAddress,
            ),
            to = ExchangeIntent.Asset(
                network = intent.toNetwork,
                contractAddress = intent.toContractAddress,
                decimals = intent.toDecimals,
                address = intent.payoutAddress,
            ),
            providerId = intent.provider,
            rateType = when (intent.rateType) {
                RateType.FLOAT -> ExpressRateType.Float
                RateType.FIXED -> ExpressRateType.Fixed
            },
            toExtraId = intent.toExtraId,
        )
    }
}