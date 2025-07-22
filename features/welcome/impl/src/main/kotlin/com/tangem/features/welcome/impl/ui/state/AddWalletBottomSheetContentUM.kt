package com.tangem.features.welcome.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class AddWalletBottomSheetContentUM(
    val onOptionClick: (Option) -> Unit = {},
) : TangemBottomSheetConfigContent {

    enum class Option {
        Create,
        Add,
        Buy,
    }
}