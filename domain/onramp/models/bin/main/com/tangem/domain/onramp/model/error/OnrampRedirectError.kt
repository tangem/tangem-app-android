package com.tangem.domain.onramp.model.error

sealed class OnrampRedirectError : Throwable() {

    data object VerificationFailed : OnrampRedirectError()

    data object WrongRequestId : OnrampRedirectError()
}