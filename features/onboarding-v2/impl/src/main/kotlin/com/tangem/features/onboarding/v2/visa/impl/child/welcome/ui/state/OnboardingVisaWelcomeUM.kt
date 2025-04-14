package com.tangem.features.onboarding.v2.visa.impl.child.welcome.ui.state

internal data class OnboardingVisaWelcomeUM(
    val mode: Mode = Mode.Hello,
    val continueButtonLoading: Boolean = false,
    val onContinueClick: () -> Unit = {},
) {
    enum class Mode {
        Hello, WelcomeBack
    }
}