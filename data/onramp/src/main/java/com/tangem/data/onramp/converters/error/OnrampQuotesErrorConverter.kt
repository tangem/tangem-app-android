package com.tangem.data.onramp.converters.error

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.express.models.response.ExpressError
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.utils.converter.Converter

internal class OnrampQuotesErrorConverter(
    private val jsonAdapter: JsonAdapter<ExpressErrorResponse>,
) : Converter<Pair<String, Amount>, OnrampQuote.Error?> {

    @Suppress("MagicNumber")
    override fun convert(value: Pair<String, Amount>): OnrampQuote.Error? {
        val (errorResponse, amount) = value
        try {
            val error = jsonAdapter.fromJson(errorResponse)?.error ?: return null
            return when (error.code) {
                2250 -> tryParseExchangeTooSmallAmountError(error = error, amount = amount)
                2251 -> tryParseExchangeTooBigAmountError(error = error, amount = amount)
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun tryParseExchangeTooSmallAmountError(
        error: ExpressError,
        amount: Amount,
    ): OnrampQuote.Error.AmountTooSmallError? {
        val minAmount = error.value?.minAmount ?: return null
        val decimals = error.value?.decimals ?: return null

        return OnrampQuote.Error.AmountTooSmallError(
            amount = createFromAmountWithOffset(
                amountWithOffset = minAmount,
                decimals = decimals,
                symbol = amount.currencySymbol,
                type = amount.type,
            ),
        )
    }

    private fun tryParseExchangeTooBigAmountError(
        error: ExpressError,
        amount: Amount,
    ): OnrampQuote.Error.AmountTooBigError? {
        val maxAmount = error.value?.maxAmount ?: return null
        val decimals = error.value?.decimals ?: return null

        return OnrampQuote.Error.AmountTooBigError(
            amount = createFromAmountWithOffset(
                amountWithOffset = maxAmount,
                decimals = decimals,
                symbol = amount.currencySymbol,
                type = amount.type,
            ),
        )
    }

    private fun createFromAmountWithOffset(
        amountWithOffset: String,
        decimals: Int,
        symbol: String,
        type: AmountType,
    ): Amount {
        return Amount(
            value = requireNotNull(
                amountWithOffset.toBigDecimalOrNull()?.movePointLeft(decimals),
            ) { "wrong amount format, use only digits" },
            decimals = decimals,
            currencySymbol = symbol,
            type = type,
        )
    }
}
