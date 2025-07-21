package com.tangem.domain.express.models

import java.math.BigDecimal

sealed class ExpressError : Throwable() {

    abstract val code: Int

    data class DataError(
        override val code: Int,
        val description: String?,
    ) : ExpressError()

    data class BadRequest(override val code: Int) : ExpressError()

    data class Forbidden(override val code: Int) : ExpressError()

    data class InternalError(override val code: Int) : ExpressError()

    data class ProviderNotFoundError(override val code: Int) : ExpressError()

    data class ProviderNotActiveError(override val code: Int) : ExpressError()

    data class ProviderNotAvailableError(override val code: Int) : ExpressError()

    data class ProviderInternalError(override val code: Int) : ExpressError()

    data class ExchangeNotPossibleError(override val code: Int) : ExpressError()

    data class NotEnoughBalanceError(override val code: Int) : ExpressError()

    data class InvalidAddressError(override val code: Int) : ExpressError()

    sealed class AmountError : ExpressError() {
        abstract val amount: BigDecimal

        data class TooSmallError(override val code: Int, override val amount: BigDecimal) : AmountError()
        data class TooBigError(override val code: Int, override val amount: BigDecimal) : AmountError()
        data class NotEnoughAllowanceError(override val code: Int, override val amount: BigDecimal) : AmountError()
    }

    data class ProviderDifferentAmountError(
        override val code: Int,
        val fromAmount: BigDecimal,
        val fromProviderAmount: BigDecimal,
        val decimals: Int,
    ) : ExpressError()

    data class InvalidFromDecimalsError(
        override val code: Int,
        val receivedFromDecimals: Int,
        val expressFromDecimals: Int,
    ) : ExpressError()

    data class InvalidSignatureError(override val code: Int = 990) : ExpressError()

    data class InvalidRequestIdError(override val code: Int = 991) : ExpressError()

    data class InvalidPayoutAddressError(override val code: Int = 992) : ExpressError()

    data object UnknownError : ExpressError() {
        override val code: Int = -1
    }
}