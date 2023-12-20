package com.tangem.feature.swap.converters

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.utils.converter.Converter

internal class ErrorsDataConverter(
    private val jsonAdapter: JsonAdapter<ExpressErrorResponse>,
) : Converter<String, DataError> {

    @Suppress("MagicNumber")
    override fun convert(value: String): DataError {
        try {
            val error = jsonAdapter.fromJson(value)?.error ?: return DataError.UnknownError

            return when (error.code) {
                2010 -> DataError.BadRequest(code = error.code)
                2210 -> DataError.ExchangeProviderNotFoundError(code = error.code)
                2220 -> DataError.ExchangeProviderNotActiveError(code = error.code)
                2230 -> DataError.ExchangeProviderNotAvailableError(code = error.code)
                2240 -> DataError.ExchangeNotPossibleError(code = error.code)
                2250 -> DataError.ExchangeTooSmallAmountError(
                    code = error.code,
                    amount = createFromAmountWithOffset(
                        requireNotNull(error.value?.minAmount),
                        requireNotNull(error.value?.decimals),
                    ),
                )
                2260 -> DataError.ExchangeNotEnoughAllowanceError(
                    code = error.code,
                    currentAllowance = requireNotNull(error.value?.currentAllowance),
                )
                2270 -> DataError.ExchangeNotEnoughBalanceError(code = error.code)
                2280 -> DataError.ExchangeInvalidAddressError(code = error.code)
                2290 -> DataError.ExchangeInvalidFromDecimalsError(
                    code = error.code,
                    receivedFromDecimals = requireNotNull(error.value?.receivedFromDecimals),
                    expressFromDecimals = requireNotNull(error.value?.expressFromDecimals),
                )
                else -> DataError.UnknownErrorWithCode(error.code)
            }
        } catch (e: Exception) {
            return DataError.UnknownError
        }
    }
}