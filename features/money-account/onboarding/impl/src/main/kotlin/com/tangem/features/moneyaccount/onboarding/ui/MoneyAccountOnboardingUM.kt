package com.tangem.features.moneyaccount.onboarding.ui

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class MoneyAccountOnboardingUM {

    abstract val onBack: () -> Unit

    data class Loading(override val onBack: () -> Unit) : MoneyAccountOnboardingUM()

    data class Content(
        override val onBack: () -> Unit,
        val isLoading: Boolean,
        val onGetCardClick: () -> Unit,
        val onTermsClick: () -> Unit,
        val onPrivacyClick: () -> Unit,
    ) : MoneyAccountOnboardingUM()
}