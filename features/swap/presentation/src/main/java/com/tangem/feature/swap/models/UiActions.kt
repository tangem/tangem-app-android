package com.tangem.feature.swap.models

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
    val hidePermissionBottomSheet: () -> Unit,
)
