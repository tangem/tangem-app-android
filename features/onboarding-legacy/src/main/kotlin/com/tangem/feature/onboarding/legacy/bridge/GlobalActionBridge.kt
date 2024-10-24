package com.tangem.feature.onboarding.legacy.bridge

import com.tangem.feature.onboarding.legacy.redux.OnboardingGlobalAction

interface GlobalActionBridge {

    fun dispatch(action: OnboardingGlobalAction)
}
