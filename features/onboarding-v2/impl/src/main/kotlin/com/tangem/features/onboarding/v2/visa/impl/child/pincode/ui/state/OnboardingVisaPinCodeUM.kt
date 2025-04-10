package com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class OnboardingVisaPinCodeUM(
    val pinCode: String = "",
    val error: TextReference? = null,
    val onPinCodeChange: (String) -> Unit = {},
    val submitButtonLoading: Boolean = false,
    val submitButtonEnabled: Boolean = true,
    val onSubmitClick: () -> Unit = {},
)