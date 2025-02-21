package com.tangem.feature.swap.models

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.TxFee
import java.math.BigDecimal

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
    val onReduceToAmount: (SwapAmount) -> Unit,
    val onReduceByAmount: (SwapAmount, reduceBy: BigDecimal) -> Unit,
    val openPermissionBottomSheet: () -> Unit,
    val onChangeApproveType: (ApproveType) -> Unit,
    // region new actions
    val onStoriesClose: (Int) -> Unit,
    val onRetryClick: () -> Unit,
    val onClickFee: () -> Unit,
    val onSelectFeeType: (TxFee) -> Unit,
    val onProviderClick: (String) -> Unit,
    val onProviderSelect: (String) -> Unit,
    val onBuyClick: (CryptoCurrency) -> Unit,
    val onSelectTokenClick: () -> Unit,
    val onSuccess: () -> Unit,
    val onLinkClick: (String) -> Unit,
    val onReceiveCardWarningClick: () -> Unit,
)