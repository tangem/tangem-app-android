package com.tangem.features.onboarding.v2.multiwallet.impl.ui.state

import com.tangem.features.onboarding.v2.common.ui.OnboardingDialogUM

internal data class OnboardingMultiWalletUM(
    val artwork1Url: String? = null,
    val artwork2Url: String? = null,
    val artwork3Url: String? = null,
    val dialog: OnboardingDialogUM? = null,
)