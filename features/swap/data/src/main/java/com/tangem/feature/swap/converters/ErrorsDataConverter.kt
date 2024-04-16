package com.tangem.feature.swap.converters

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.express.models.response.ExpressError
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.utils.converter.Converter

internal class ErrorsDataConverter(
    private val jsonAdapter: JsonAdapter<ExpressErrorResponse>,
) : Converter<String, DataError> {

    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    override fun convert(value: String): DataError {
        try {
            val error = jsonAdapter.fromJson(value)?.error ?: return DataError.UnknownError

            return when (error.code) {
                2010 -> DataError.BadRequest(code = error.code)
                2200 -> DataError.SwapsAreUnavailableNowError(code = error.code)
                2210 -> DataError.ExchangeProviderNotFoundError(code = error.code)
                2220 -> DataError.ExchangeProviderNotActiveError(code = error.code)
                2230 -> DataError.ExchangeProviderNotAvailableError(code = error.code)
                2231 -> DataError.ExchangeProviderProviderInternalError(code = error.code)
                2240 -> DataError.ExchangeNotPossibleError(code = error.code)
                2250 -> tryParseExchangeTooSmallAmountError(error = error)
                2251 -> tryParseExchangeTooBigAmountError(error = error)
                2260 -> tryParseExchangeNotEnoughAllowanceError(error = error)
                2270 -> DataError.ExchangeNotEnoughBalanceError(code = error.code)
                2280 -> DataError.ExchangeInvalidAddressError(code = error.code)
                2290 -> tryParseExchangeInvalidFromDecimalsError(error = error)
                else -> DataError.UnknownErrorWithCode(error.code)
            }
        } catch (e: Exception) {
            return DataError.UnknownError
        }
    }

    private fun tryParseExchangeTooSmallAmountError(error: ExpressError): DataError {
        val minAmount = error.value?.minAmount ?: return DataError.UnknownErrorWithCode(error.code)
        val decimals = error.value?.decimals ?: return DataError.UnknownErrorWithCode(error.code)

        return DataError.ExchangeTooSmallAmountError(
            code = error.code,
            amount = createFromAmountWithOffset(minAmount, decimals),
        )
    }

    private fun tryParseExchangeTooBigAmountError(error: ExpressError): DataError {
        val minAmount = error.value?.maxAmount ?: return DataError.UnknownErrorWithCode(error.code)
        val decimals = error.value?.decimals ?: return DataError.UnknownErrorWithCode(error.code)

        return DataError.ExchangeTooBigAmountError(
            code = error.code,
            amount = createFromAmountWithOffset(minAmount, decimals),
        )
    }

    private fun tryParseExchangeNotEnoughAllowanceError(error: ExpressError): DataError {
        val currentAllowance = error.value?.currentAllowance ?: return DataError.UnknownErrorWithCode(error.code)

        return DataError.ExchangeNotEnoughAllowanceError(
            code = error.code,
            currentAllowance = currentAllowance,
        )
    }

    private fun tryParseExchangeInvalidFromDecimalsError(error: ExpressError): DataError {
        val receivedFromDecimals = error.value?.receivedFromDecimals ?: return DataError.UnknownErrorWithCode(
            code = error.code,
        )
        val expressFromDecimals = error.value?.expressFromDecimals ?: return DataError.UnknownErrorWithCode(error.code)

        return DataError.ExchangeInvalidFromDecimalsError(
            code = error.code,
            receivedFromDecimals = receivedFromDecimals,
            expressFromDecimals = expressFromDecimals,
        )
    }
}