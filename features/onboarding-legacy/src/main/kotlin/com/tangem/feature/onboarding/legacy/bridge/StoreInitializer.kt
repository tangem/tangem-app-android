package com.tangem.feature.onboarding.legacy.bridge

import com.tangem.feature.onboarding.legacy.redux.DaggerGraphState
import com.tangem.feature.onboarding.legacy.redux.OnboardingReduxState
import com.tangem.feature.onboarding.legacy.redux.store.onboardingReducer
import com.tangem.feature.onboarding.legacy.redux.store.store
import org.rekotlin.Store

object StoreInitializer {

    fun init(daggerGraphState: DaggerGraphState) {
        store = Store(
            reducer = { action, state -> onboardingReducer(action, state) },
            middleware = OnboardingReduxState.getMiddleware(),
            state = OnboardingReduxState(
                daggerGraphState = daggerGraphState,
            ),
        )
    }
}