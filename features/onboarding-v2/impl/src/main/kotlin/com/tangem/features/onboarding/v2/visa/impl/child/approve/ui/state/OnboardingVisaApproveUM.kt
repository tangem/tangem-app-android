package com.tangem.features.onboarding.v2.visa.impl.child.approve.ui.state

internal data class OnboardingVisaApproveUM(
    val approveButtonLoading: Boolean = false,
    val onApproveClick: () -> Unit = {},
)