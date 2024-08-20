package com.tangem.features.markets.portfolio.impl.ui.state

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class AddToPortfolioBSContentUM(
    val selectedWallet: UserWalletItemUM,
    val selectNetworkUM: SelectNetworkUM,
    val isScanCardNotificationVisible: Boolean,
) : TangemBottomSheetConfigContent