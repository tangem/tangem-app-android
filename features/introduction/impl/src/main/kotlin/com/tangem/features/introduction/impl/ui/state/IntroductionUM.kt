package com.tangem.features.introduction.impl.ui.state

internal data class IntroductionUM(
    val isVideoReady: Boolean,
    val isMotionEnabled: Boolean,
    val onCreateWalletClick: () -> Unit,
    val onIHaveWalletClick: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
)