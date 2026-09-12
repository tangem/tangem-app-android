package com.tangem.features.tangempay.eligibility.wallet

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayWalletSelectorUM(
    val userWallets: ImmutableList<UserWalletItemUM>,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent