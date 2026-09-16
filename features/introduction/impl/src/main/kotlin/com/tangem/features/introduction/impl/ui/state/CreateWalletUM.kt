package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class CreateWalletUM(
    val cardOrRing: WalletOptionUM,
    val seedPhrase: WalletOptionUM,
    val onCloseClick: () -> Unit,
) : TangemBottomSheetConfigContent {

    val isCreatingWallet: Boolean
        get() = seedPhrase.isLoading
}