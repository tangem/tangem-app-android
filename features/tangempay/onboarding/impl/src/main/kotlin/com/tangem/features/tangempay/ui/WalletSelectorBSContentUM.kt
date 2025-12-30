package com.tangem.features.tangempay.ui

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class WalletSelectorBSContentUM(
    val userWallets: ImmutableList<UserWalletItemUM>,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent