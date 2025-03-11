package com.tangem.feature.swap.converters

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.express.models.response.ExpressError
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.utils.converter.Converter

internal class ErrorsDataConverter(
    private val jsonAdapter: JsonAdapter<ExpressErrorResponse>,
) : Converter<String, ExpressDataError> {

    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    override fun convert(value: String): ExpressDataError {
        try {
            val error = jsonAdapter.fromJson(value)?.error ?: return ExpressDataError.UnknownError

            return when (error.code) {
                2010 -> ExpressDataError.BadRequest(code = error.code)
                2200 -> ExpressDataError.SwapsAreUnavailableNowError(code = error.code)
                2210 -> ExpressDataError.ExchangeProviderNotFoundError(code = error.code)
                2220 -> ExpressDataError.ExchangeProviderNotActiveError(code = error.code)
                2230 -> ExpressDataError.ExchangeProviderNotAvailableError(code = error.code)
                2231 -> ExpressDataError.ExchangeProviderProviderInternalError(code = error.code)
                2240 -> ExpressDataError.ExchangeNotPossibleError(code = error.code)
                2250 -> tryParseExchangeTooSmallAmountError(error = error)
                2251 -> tryParseExchangeTooBigAmountError(error = error)
                2260 -> tryParseExchangeNotEnoughAllowanceError(error = error)
                2270 -> ExpressDataError.ExchangeNotEnoughBalanceError(code = error.code)
                2280 -> ExpressDataError.ExchangeInvalidAddressError(code = error.code)
                2290 -> tryParseExchangeInvalidFromDecimalsError(error = error)
                2320 -> tryParseProviderDifferentAmountError(error = error)
                else -> ExpressDataError.UnknownErrorWithCode(error.code)
            }
        } catch (e: Exception) {
            return ExpressDataError.UnknownError
        }
    }

    private fun tryParseExchangeTooSmallAmountError(error: ExpressError): ExpressDataError {
        val minAmount = error.value?.minAmount ?: return ExpressDataError.UnknownErrorWithCode(error.code)
        val decimals = error.value?.decimals ?: return ExpressDataError.UnknownErrorWithCode(error.code)

        return ExpressDataError.ExchangeTooSmallAmountError(
            code = error.code,
            amount = createFromAmountWithOffset(minAmount, decimals),
        )
    }

    private fun tryParseExchangeTooBigAmountError(error: ExpressError): ExpressDataError {
        val minAmount = error.value?.maxAmount ?: return ExpressDataError.UnknownErrorWithCode(error.code)
        val decimals = error.value?.decimals ?: return ExpressDataError.UnknownErrorWithCode(error.code)

        return ExpressDataError.ExchangeTooBigAmountError(
            code = error.code,
            amount = createFromAmountWithOffset(minAmount, decimals),
        )
    }

    private fun tryParseExchangeNotEnoughAllowanceError(error: ExpressError): ExpressDataError {
        val currentAllowance = error.value?.currentAllowance ?: return ExpressDataError.UnknownErrorWithCode(error.code)

        return ExpressDataError.ExchangeNotEnoughAllowanceError(
            code = error.code,
            currentAllowance = currentAllowance,
        )
    }

    private fun tryParseExchangeInvalidFromDecimalsError(error: ExpressError): ExpressDataError {
        val receivedFromDecimals = error.value?.receivedFromDecimals ?: return ExpressDataError.UnknownErrorWithCode(
            code = error.code,
        )
        val expressFromDecimals =
            error.value?.expressFromDecimals ?: return ExpressDataError.UnknownErrorWithCode(error.code)

        return ExpressDataError.ExchangeInvalidFromDecimalsError(
            code = error.code,
            receivedFromDecimals = receivedFromDecimals,
            expressFromDecimals = expressFromDecimals,
        )
    }

    private fun tryParseProviderDifferentAmountError(error: ExpressError): ExpressDataError {
        val decimals = error.value?.decimals ?: return ExpressDataError.UnknownErrorWithCode(error.code)

        val fromAmount = error.value?.fromAmount?.toBigDecimalOrNull()
            ?: return ExpressDataError.UnknownErrorWithCode(error.code)
        val fromAmountProvider = error.value?.fromAmountProvider?.toBigDecimalOrNull()
            ?: return ExpressDataError.UnknownErrorWithCode(error.code)

        return ExpressDataError.ProviderDifferentAmountError(
            code = error.code,
            decimals = decimals,
            fromAmount = fromAmount.movePointLeft(decimals),
            fromProviderAmount = fromAmountProvider.movePointLeft(decimals),
        )
    }
}