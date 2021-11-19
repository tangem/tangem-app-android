package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Middleware

class OnboardingWalletMiddleware {
    companion object {
        val handler = onboardingWalletMiddleware
    }
}

private val onboardingWalletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            next(action)
        }
    }
}