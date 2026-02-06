package com.tangem.features.feed.components.market.details.portfolio.impl.ui.state

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class WalletSelectorBSContentUM(
    val userWallets: ImmutableList<UserWalletItemUM>,
    val onBack: () -> Unit,
) : TangemBottomSheetConfigContent