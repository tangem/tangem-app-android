package com.tangem.domain.onramp.model.error

import java.math.BigDecimal

sealed class OnrampError {

    sealed class AmountError : OnrampError() {
        abstract val requiredAmount: BigDecimal

        data class TooSmallError(
            override val requiredAmount: BigDecimal,
        ) : AmountError()

        data class TooBigError(
            override val requiredAmount: BigDecimal,
        ) : AmountError()
    }

    sealed class RedirectError : OnrampError() {
        data object VerificationFailed : RedirectError()
        data object WrongRequestId : RedirectError()
    }

    data class DataError(
        val code: String,
        val description: String?,
    ) : OnrampError()

    data class DomainError(
        val description: String?,
    ) : OnrampError()

    data object PairsNotFound : OnrampError()
}