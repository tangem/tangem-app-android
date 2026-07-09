package com.tangem.features.virtualaccount.onboarding.ui

import androidx.compose.runtime.Immutable

/**
 * UI model for the Virtual Account onboarding screen.
 */
@Immutable
internal sealed class VirtualAccountOnboardingUM {

    abstract val onBack: () -> Unit

    data class Loading(override val onBack: () -> Unit) : VirtualAccountOnboardingUM()

    data class Content(
        override val onBack: () -> Unit,
        val isLoading: Boolean,
        val onGetCardClick: () -> Unit,
        val onTermsClick: () -> Unit,
        val onPrivacyClick: () -> Unit,
    ) : VirtualAccountOnboardingUM()
}