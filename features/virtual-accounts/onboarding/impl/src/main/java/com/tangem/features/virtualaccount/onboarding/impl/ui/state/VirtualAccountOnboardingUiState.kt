package com.tangem.features.virtualaccount.onboarding.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
data class VirtualAccountOnboardingUiState(
    val termsOfUseUrl: String = "https://tangem.com",
    val privacyPolicyLink: String = "https://tangem.com",
)