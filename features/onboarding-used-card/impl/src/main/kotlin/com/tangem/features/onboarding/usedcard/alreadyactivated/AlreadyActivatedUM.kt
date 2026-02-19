package com.tangem.features.onboarding.usedcard.alreadyactivated

internal data class AlreadyActivatedUM(
    val isSavingWallet: Boolean,
    val onThisIsMyWalletClick: () -> Unit,
    val onNewCardClick: () -> Unit,
)