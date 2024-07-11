package com.tangem.domain.card.models

sealed class ResetCardError {

    data object UserCanceled : ResetCardError()

    data object SdkError : ResetCardError()
}