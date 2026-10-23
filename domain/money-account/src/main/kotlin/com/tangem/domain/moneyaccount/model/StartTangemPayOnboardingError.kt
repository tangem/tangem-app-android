package com.tangem.domain.moneyaccount.model

sealed interface StartTangemPayOnboardingError {
    data object UserCancelled : StartTangemPayOnboardingError
    data object Failed : StartTangemPayOnboardingError
}