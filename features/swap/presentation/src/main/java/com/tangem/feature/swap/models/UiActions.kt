package com.tangem.feature.swap.models

import com.tangem.feature.swap.domain.models.ui.TxFee

data class UiActions(
    val onSearchEntered: (String) -> Unit,
    val onTokenSelected: (String) -> Unit,
    val onAmountChanged: (String) -> Unit,
    val onAmountSelected: (Boolean) -> Unit,
    val onSwapClick: () -> Unit,
    val onGivePermissionClick: () -> Unit,
    val onChangeCardsClicked: () -> Unit,
    val onBackClicked: () -> Unit,
    val onMaxAmountSelected: () -> Unit,
    val openPermissionBottomSheet: () -> Unit,
    val onChangeApproveType: (ApproveType) -> Unit,
    // region new actions
    val onRetryClick: () -> Unit,
    val onClickFee: () -> Unit,
    val onSelectFeeType: (TxFee) -> Unit,
    val onProviderClick: (String) -> Unit,
    val onProviderSelect: (String) -> Unit,
    val onBuyClick: () -> Unit,
    val onPolicyClick: (String) -> Unit,
    val onTosClick: (String) -> Unit,
    val onReceiveCardWarningClick: () -> Unit,
)