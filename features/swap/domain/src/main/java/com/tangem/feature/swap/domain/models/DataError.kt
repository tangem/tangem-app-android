package com.tangem.feature.swap.domain.models

import java.math.BigDecimal

sealed class DataError {

    abstract val code: Int

    data class BadRequest(override val code: Int) : DataError()

    data class ExchangeProviderNotFoundError(override val code: Int) : DataError()

    data class ExchangeProviderNotActiveError(override val code: Int) : DataError()

    data class ExchangeProviderNotAvailableError(override val code: Int) : DataError()

    data class ExchangeNotPossibleError(override val code: Int) : DataError()

    data class ExchangeTooSmallAmountError(override val code: Int, val amount: SwapAmount) : DataError()

    data class ExchangeNotEnoughAllowanceError(override val code: Int, val currentAllowance: BigDecimal) : DataError()

    data class ExchangeNotEnoughBalanceError(override val code: Int) : DataError()

    data class ExchangeInvalidAddressError(override val code: Int) : DataError()

    data class ExchangeInvalidFromDecimalsError(
        override val code: Int,
        val receivedFromDecimals: Int,
        val expressFromDecimals: Int,
    ) : DataError()

    data class UnknownError(override val code: Int = -1) : DataError()
}
