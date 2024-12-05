package com.tangem.data.onramp.converters.error

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.express.models.response.ExpressError
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero

internal class OnrampQuotesErrorConverter(
    private val jsonAdapter: JsonAdapter<ExpressErrorResponse>,
) : Converter<OnrampQuoteErrorInput, OnrampQuote.Error?> {

    @Suppress("MagicNumber")
    override fun convert(value: OnrampQuoteErrorInput): OnrampQuote.Error? {
        try {
            val error = jsonAdapter.fromJson(value.errorBody)?.error ?: return null
            return when (error.code) {
                2250 -> tryParseExchangeTooSmallAmountError(error = error, input = value)
                2251 -> tryParseExchangeTooBigAmountError(error = error, input = value)
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun tryParseExchangeTooSmallAmountError(
        error: ExpressError,
        input: OnrampQuoteErrorInput,
    ): OnrampQuote.Error.AmountTooSmallError? {
        val minAmount = error.value?.minAmount ?: return null
        val decimals = error.value?.decimals ?: return null

        return OnrampQuote.Error.AmountTooSmallError(
            paymentMethod = input.paymentMethod,
            provider = input.provider,
            fromAmount = OnrampAmount(
                symbol = input.amount.currencySymbol,
                value = input.amount.value.orZero(),
                decimals = input.amount.decimals,
            ),
            requiredAmount = createFromAmountWithOffset(
                amountWithOffset = minAmount,
                decimals = decimals,
                symbol = input.amount.currencySymbol,
            ),
        )
    }

    private fun tryParseExchangeTooBigAmountError(
        error: ExpressError,
        input: OnrampQuoteErrorInput,
    ): OnrampQuote.Error.AmountTooBigError? {
        val maxAmount = error.value?.maxAmount ?: return null
        val decimals = error.value?.decimals ?: return null

        return OnrampQuote.Error.AmountTooBigError(
            paymentMethod = input.paymentMethod,
            provider = input.provider,
            fromAmount = OnrampAmount(
                symbol = input.amount.currencySymbol,
                value = input.amount.value.orZero(),
                decimals = input.amount.decimals,
            ),
            requiredAmount = createFromAmountWithOffset(
                amountWithOffset = maxAmount,
                decimals = decimals,
                symbol = input.amount.currencySymbol,
            ),
        )
    }

    private fun createFromAmountWithOffset(amountWithOffset: String, decimals: Int, symbol: String): OnrampAmount {
        return OnrampAmount(
            value = requireNotNull(
                amountWithOffset.toBigDecimalOrNull()?.movePointLeft(decimals),
            ) { "wrong amount format, use only digits" },
            decimals = decimals,
            symbol = symbol,
        )
    }
}