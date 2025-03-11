package com.tangem.domain.redux

import org.rekotlin.Action

sealed class OnboardingManageTokensAction : Action {
    data object CurrenciesSaved : OnboardingManageTokensAction()
}