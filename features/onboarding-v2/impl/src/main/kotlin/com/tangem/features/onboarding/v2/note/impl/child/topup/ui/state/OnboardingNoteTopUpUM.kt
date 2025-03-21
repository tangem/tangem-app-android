package com.tangem.features.onboarding.v2.note.impl.child.topup.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig

data class OnboardingNoteTopUpUM(
    val cardArtworkUrl: String? = null,
    val availableForBuy: Boolean = false,
    val availableForBuyLoading: Boolean = true,
    val balance: String = "–",
    val isRefreshing: Boolean = false,
    val isTopUpDataLoading: Boolean = true,
    val amountToCreateAccount: String? = null,
    val addressBottomSheetConfig: TangemBottomSheetConfig? = null,
    val onBuyCryptoClick: () -> Unit = {},
    val onShowWalletAddressClick: () -> Unit = {},
    val onRefreshBalanceClick: () -> Unit = {},
    val onDismissBottomSheet: () -> Unit = {},
)