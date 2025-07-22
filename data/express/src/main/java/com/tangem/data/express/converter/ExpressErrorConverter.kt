package com.tangem.data.express.converter

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.domain.express.models.ExpressError
import com.tangem.utils.converter.Converter
import com.tangem.datasource.api.express.models.response.ExpressError as ExpressErrorDTO

class ExpressErrorConverter(
    private val jsonAdapter: JsonAdapter<ExpressErrorResponse>,
) : Converter<String, ExpressError> {

    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    override fun convert(value: String): ExpressError {
        try {
            val error: ExpressErrorDTO = jsonAdapter.fromJson(value)?.error ?: return ExpressError.UnknownError

            return when (error.code) {
                2010 -> ExpressError.BadRequest(code = error.code)
                2020 -> ExpressError.Forbidden(code = error.code)

                2200 -> ExpressError.InternalError(code = error.code)
                2210 -> ExpressError.ProviderNotFoundError(code = error.code)
                2220 -> ExpressError.ProviderNotActiveError(code = error.code)
                2230 -> ExpressError.ProviderNotAvailableError(code = error.code)
                2231 -> ExpressError.ProviderInternalError(code = error.code)
                2240 -> ExpressError.ExchangeNotPossibleError(code = error.code)
                2250 -> error.toTooSmallAmountError()
                2251 -> error.toTooBigAmountError()
                2260 -> error.toNotEnoughAllowanceError()
                2270 -> ExpressError.NotEnoughBalanceError(code = error.code)
                2280 -> ExpressError.InvalidAddressError(code = error.code)
                2290 -> error.toInvalidFromDecimalsError()

                2320 -> error.toProviderDifferentAmountError()
                else -> ExpressError.DataError(error.code, error.description)
            }
        } catch (e: Exception) {
            return ExpressError.UnknownError
        }
    }

    private fun ExpressErrorDTO.toTooSmallAmountError(): ExpressError {
        val minAmount = value?.minAmount ?: return ExpressError.DataError(code, description)
        val decimals = value?.decimals ?: return ExpressError.DataError(code, description)
        val requiredAmount = minAmount.toBigDecimalOrNull()?.movePointLeft(decimals)
            ?: return ExpressError.DataError(code, description)

        return ExpressError.AmountError.TooSmallError(code = code, amount = requiredAmount)
    }

    private fun ExpressErrorDTO.toTooBigAmountError(): ExpressError {
        val maxAmount = value?.maxAmount ?: return ExpressError.DataError(code, description)
        val decimals = value?.decimals ?: return ExpressError.DataError(code, description)
        val requiredAmount = maxAmount.toBigDecimalOrNull()?.movePointLeft(decimals)
            ?: return ExpressError.DataError(code, description)

        return ExpressError.AmountError.TooBigError(code = code, amount = requiredAmount)
    }

    private fun ExpressErrorDTO.toNotEnoughAllowanceError(): ExpressError {
        val currentAllowance = value?.currentAllowance ?: return ExpressError.DataError(code, description)

        return ExpressError.AmountError.NotEnoughAllowanceError(code = code, amount = currentAllowance)
    }

    private fun ExpressErrorDTO.toInvalidFromDecimalsError(): ExpressError {
        val receivedFromDecimals = value?.receivedFromDecimals ?: return ExpressError.DataError(code, description)
        val expressFromDecimals = value?.expressFromDecimals ?: return ExpressError.DataError(code, description)

        return ExpressError.InvalidFromDecimalsError(
            code = code,
            receivedFromDecimals = receivedFromDecimals,
            expressFromDecimals = expressFromDecimals,
        )
    }

    private fun ExpressErrorDTO.toProviderDifferentAmountError(): ExpressError {
        val decimals = value?.decimals ?: return ExpressError.DataError(code, description)
        val fromAmount = value?.fromAmount?.toBigDecimalOrNull() ?: return ExpressError.DataError(code, description)
        val fromAmountProvider = value?.fromAmountProvider?.toBigDecimalOrNull()
            ?: return ExpressError.DataError(code, description)

        return ExpressError.ProviderDifferentAmountError(
            code = code,
            decimals = decimals,
            fromAmount = fromAmount.movePointLeft(decimals),
            fromProviderAmount = fromAmountProvider.movePointLeft(decimals),
        )
    }
}