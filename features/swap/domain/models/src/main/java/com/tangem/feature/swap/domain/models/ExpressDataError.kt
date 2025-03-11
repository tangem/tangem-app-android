package com.tangem.feature.swap.domain.models

import java.math.BigDecimal

sealed class ExpressDataError {

    abstract val code: Int

    data class BadRequest(override val code: Int) : ExpressDataError()

    data class SwapsAreUnavailableNowError(override val code: Int) : ExpressDataError()

    data class ExchangeProviderNotFoundError(override val code: Int) : ExpressDataError()

    data class ExchangeProviderNotActiveError(override val code: Int) : ExpressDataError()

    data class ExchangeProviderNotAvailableError(override val code: Int) : ExpressDataError()

    data class ExchangeProviderProviderInternalError(override val code: Int) : ExpressDataError()

    data class ExchangeNotPossibleError(override val code: Int) : ExpressDataError()

    data class ExchangeTooSmallAmountError(override val code: Int, val amount: SwapAmount) : ExpressDataError()

    data class ExchangeTooBigAmountError(override val code: Int, val amount: SwapAmount) : ExpressDataError()

    data class ExchangeNotEnoughAllowanceError(
        override val code: Int,
        val currentAllowance: BigDecimal,
    ) : ExpressDataError()

    data class ExchangeNotEnoughBalanceError(override val code: Int) : ExpressDataError()

    data class ExchangeInvalidAddressError(override val code: Int) : ExpressDataError()

    data class ExchangeInvalidFromDecimalsError(
        override val code: Int,
        val receivedFromDecimals: Int,
        val expressFromDecimals: Int,
    ) : ExpressDataError()

    data class ProviderDifferentAmountError(
        override val code: Int,
        val fromAmount: BigDecimal,
        val fromProviderAmount: BigDecimal,
        val decimals: Int,
    ) : ExpressDataError()

    data class UnknownErrorWithCode(override val code: Int) : ExpressDataError()

    data class InvalidSignatureError(override val code: Int = 990) : ExpressDataError()

    data class InvalidRequestIdError(override val code: Int = 991) : ExpressDataError()

    data class InvalidPayoutAddressError(override val code: Int = 992) : ExpressDataError()

    data object UnknownError : ExpressDataError() {
        override val code: Int = -1
    }
}