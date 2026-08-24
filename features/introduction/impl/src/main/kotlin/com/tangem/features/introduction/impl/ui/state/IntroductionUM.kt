package com.tangem.features.introduction.impl.ui.state

internal data class IntroductionUM(
    val onCreateWalletClick: () -> Unit,
    val onIHaveWalletClick: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
)