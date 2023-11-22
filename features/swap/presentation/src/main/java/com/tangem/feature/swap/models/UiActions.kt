package com.tangem.feature.swap.models

import com.tangem.core.ui.components.states.Item
import com.tangem.feature.swap.domain.models.ui.FeeType
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
    val hidePermissionBottomSheet: () -> Unit,
    val onChangeApproveType: (ApproveType) -> Unit,
    val onSelectItemFee: (Item<TxFee>) -> Unit,
    // region new actions
    val onClickFee: () -> Unit,
    val onSelectFeeType: (FeeType) -> Unit,
    val onProviderClick: (String) -> Unit,
    val onProviderSelect: (String) -> Unit,
)