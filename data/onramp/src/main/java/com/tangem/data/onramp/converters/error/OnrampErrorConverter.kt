package com.tangem.data.onramp.converters.error

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.express.models.response.ExpressError
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.utils.converter.Converter

@Deprecated("Use ExpressErrorConverter")
internal class OnrampErrorConverter(
    private val jsonAdapter: JsonAdapter<ExpressErrorResponse>,
) : Converter<String, OnrampError> {

    @Suppress("MagicNumber")
    override fun convert(value: String): OnrampError {
        try {
            val error = jsonAdapter.fromJson(value)?.error ?: return OnrampError.DomainError(value)
            return when (error.code) {
                2250 -> tryParseExchangeTooSmallAmountError(error = error)
                2251 -> tryParseExchangeTooBigAmountError(error = error)
                else -> OnrampError.DataError(error.code.toString(), error.description)
            }
        } catch (e: Exception) {
            return OnrampError.DomainError(e.message)
        }
    }

    private fun tryParseExchangeTooSmallAmountError(error: ExpressError): OnrampError {
        val minAmount = error.value?.minAmount ?: return OnrampError.DataError(error.code.toString(), error.description)
        val decimals = error.value?.decimals ?: return OnrampError.DataError(error.code.toString(), error.description)
        val requiredAmount = minAmount.toBigDecimalOrNull()?.movePointLeft(decimals)
            ?: return OnrampError.DataError(error.code.toString(), error.description)

        return OnrampError.AmountError.TooSmallError(requiredAmount = requiredAmount)
    }

    private fun tryParseExchangeTooBigAmountError(error: ExpressError): OnrampError {
        val maxAmount = error.value?.maxAmount ?: return OnrampError.DataError(error.code.toString(), error.description)
        val decimals = error.value?.decimals ?: return OnrampError.DataError(error.code.toString(), error.description)
        val requiredAmount = maxAmount.toBigDecimalOrNull()?.movePointLeft(decimals)
            ?: return OnrampError.DataError(error.code.toString(), error.description)

        return OnrampError.AmountError.TooBigError(requiredAmount = requiredAmount)
    }
}