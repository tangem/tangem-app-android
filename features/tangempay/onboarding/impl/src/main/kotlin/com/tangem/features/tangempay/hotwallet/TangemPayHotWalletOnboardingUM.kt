package com.tangem.features.tangempay.hotwallet

import com.tangem.utils.annotations.RemoveWithToggle

internal data class TangemPayHotWalletOnboardingUM(
    @RemoveWithToggle("TWI_1728_PAY_BUNDLE_INTRODUCTION_ENABLED")
    val isPayBundleIntroductionEnabled: Boolean,
    @RemoveWithToggle("TWI_1525_MONEY_ACCOUNTS_ENABLED")
    val isUnifiedOnboarding: Boolean,
    val isLoading: Boolean,
    val onGetCardClick: () -> Unit,
    val onTermsClick: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
)