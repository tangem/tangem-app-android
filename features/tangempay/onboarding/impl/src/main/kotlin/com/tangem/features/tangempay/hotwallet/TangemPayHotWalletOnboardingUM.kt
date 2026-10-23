package com.tangem.features.tangempay.hotwallet

import com.tangem.utils.annotations.RemoveWithToggle

internal data class TangemPayHotWalletOnboardingUM(
    @RemoveWithToggle("TWI_1728_PAY_BUNDLE_INTRODUCTION_ENABLED")
    val isPayBundleIntroductionEnabled: Boolean,
    val isLoading: Boolean,
    val onGetCardClick: () -> Unit,
    val onTermsClick: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
)