package com.tangem.features.onboarding.v2.note.impl.child.topup.ui.state

data class OnboardingNoteTopUpUM(
    val cardArtworkUrl: String? = null,
    val availableForBuy: Boolean = true,
    val balance: String? = null,
    val isRefreshing: Boolean = false,
    val isTopUpDataLoading: Boolean = true,
    val amountToCreateAccount: String? = null,
    val onBuyCryptoClick: () -> Unit = {},
    val onShowWalletAddressClick: () -> Unit = {},
    val onRefreshBalanceClick: () -> Unit = {},
)