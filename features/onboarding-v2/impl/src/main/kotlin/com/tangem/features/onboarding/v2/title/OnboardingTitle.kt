package com.tangem.features.onboarding.v2.title

import com.tangem.core.ui.extensions.TextReference

data class OnboardingTitle(
    val text: TextReference,
    val shouldForceTitle: Boolean = false,
)