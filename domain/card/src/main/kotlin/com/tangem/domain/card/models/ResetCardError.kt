package com.tangem.domain.card.models

sealed interface ResetCardError {

    data object UserCanceled : ResetCardError

    data object AnotherSdkError : ResetCardError
}