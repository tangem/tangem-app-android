package com.tangem.features.onboarding.v2

import com.tangem.features.onboarding.v2.title.OnboardingTitle
import kotlinx.coroutines.flow.StateFlow

interface TitleProvider {
    val currentTitle: StateFlow<OnboardingTitle>
    fun changeTitle(title: OnboardingTitle)
}