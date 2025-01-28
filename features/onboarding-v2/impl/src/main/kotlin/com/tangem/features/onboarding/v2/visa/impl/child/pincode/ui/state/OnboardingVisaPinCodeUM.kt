package com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui.state

internal data class OnboardingVisaPinCodeUM(
    val pinCode: String = "",
    val onPinCodeChange: (String) -> Unit = {},
    val submitButtonLoading: Boolean = false,
    val submitButtonEnabled: Boolean = true,
    val onSubmitClick: () -> Unit = {},
)