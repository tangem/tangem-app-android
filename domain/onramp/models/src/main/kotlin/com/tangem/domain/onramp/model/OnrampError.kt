package com.tangem.domain.onramp.model

sealed class OnrampError {
    data object UnknownError : OnrampError()
}
