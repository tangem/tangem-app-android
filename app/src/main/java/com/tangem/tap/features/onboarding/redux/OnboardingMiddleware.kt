package com.tangem.tap.features.onboarding.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.store
import org.rekotlin.Middleware

class OnboardingMiddleware {
    companion object {
        val handler = onboardingMiddleware
    }
}

private val onboardingMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            val onboardingState = store.state.onboardingState
            when (action) {
            }
            next(action)
        }
    }
}