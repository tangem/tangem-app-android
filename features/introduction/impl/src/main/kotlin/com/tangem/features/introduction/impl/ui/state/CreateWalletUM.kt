package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class CreateWalletUM(
    val cardOrRing: WalletOptionUM,
    val socialLogin: WalletOptionUM?,
    val seedPhrase: WalletOptionUM,
    val onCloseClick: () -> Unit,
) : TangemBottomSheetConfigContent {

    /** Dismissing the sheet does not cancel what the tapped row started, so it stays locked until then */
    val isCreatingWallet: Boolean
        get() = seedPhrase.isLoading || socialLogin?.isLoading == true
}