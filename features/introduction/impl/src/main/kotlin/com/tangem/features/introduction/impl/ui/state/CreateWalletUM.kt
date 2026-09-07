package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class CreateWalletUM(
    val cardOrRing: WalletOptionUM,
    val seedPhrase: WalletOptionUM,
) : TangemBottomSheetConfigContent {

    val isCreatingWallet: Boolean
        get() = seedPhrase.isLoading
}