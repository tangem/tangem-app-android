package com.tangem.domain.onramp.model

sealed class OnrampRedirectError : Throwable() {

    data class DataError(override val cause: Throwable?) : OnrampRedirectError()

    data object VerificationFailed : OnrampRedirectError()
}
