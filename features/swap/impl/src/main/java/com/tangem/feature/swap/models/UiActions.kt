package com.tangem.feature.swap.models

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.domain.swap.models.PredefinedPercentAmount
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import java.math.BigDecimal

internal data class UiActions(
    val onAmountChanged: (String) -> Unit,
    val onAmountSelected: (Boolean) -> Unit,
    val onSwapClick: () -> Unit,
    val onTransferClick: () -> Unit,
    val onChangeCardsClicked: () -> Unit,
    val onBackClicked: () -> Unit,
    val onMaxAmountSelected: () -> Unit,
    val onPredefinedPercentSelected: (PredefinedPercentAmount) -> Unit,
    val onReduceToAmount: (SwapAmount) -> Unit,
    val onReduceByAmount: (SwapAmount, reduceBy: BigDecimal) -> Unit,
    val openPermissionBottomSheet: () -> Unit,
    // region new actions
    val onRetryClick: () -> Unit,
    val onProviderClick: (String) -> Unit,
    val onProviderSelect: (String) -> Unit,
    val onProviderFilterSelect: (ProviderFilterType) -> Unit,
    val openTokenDetailsScreen: (CryptoCurrency) -> Unit,
    val onSelectTokenClick: (TokenSelectionDirection) -> Unit,
    val onSuccess: () -> Unit,
    val onLinkClick: (String) -> Unit,
    val onReceiveCardWarningClick: () -> Unit,
    val onSwapUIModeChange: (SwapUIMode) -> Unit,
    val onSwapTypeMenuOpened: () -> Unit,
)