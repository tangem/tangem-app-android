package com.tangem.domain.onramp.model.error

sealed class OnrampRedirectError : Throwable() {

    class VerificationFailed : OnrampRedirectError()

    class WrongRequestId : OnrampRedirectError()
}