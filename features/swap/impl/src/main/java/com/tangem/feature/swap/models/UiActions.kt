package com.tangem.feature.swap.models

import com.tangem.feature.swap.domain.models.SwapAmount
import java.math.BigDecimal

internal data class UiActions(
    val onAmountChanged: (String) -> Unit,
    val onAmountSelected: (Boolean) -> Unit,
    val onSwapClick: () -> Unit,
    val onTransferClick: () -> Unit,
    val onChangeCardsClicked: () -> Unit,
    val onBackClicked: () -> Unit,
    val onMaxAmountSelected: () -> Unit,
    val onReduceToAmount: (SwapAmount) -> Unit,
    val onReduceByAmount: (SwapAmount, reduceBy: BigDecimal) -> Unit,
    val openPermissionBottomSheet: () -> Unit,
    // region new actions
    val onRetryClick: () -> Unit,
    val onProviderClick: (String) -> Unit,
    val onProviderSelect: (String) -> Unit,
    val onBuyClick: () -> Unit,
    val onSelectTokenClick: (TokenSelectionDirection) -> Unit,
    val onSuccess: () -> Unit,
    val onLinkClick: (String) -> Unit,
    val onReceiveCardWarningClick: () -> Unit,
)