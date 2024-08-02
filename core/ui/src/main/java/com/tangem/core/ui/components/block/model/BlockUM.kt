package com.tangem.core.ui.components.block.model

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

data class BlockUM(
    val text: TextReference,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit,
    val accentType: AccentType = AccentType.NONE,
) {

    enum class AccentType {
        NONE, ACCENT, WARNING,
    }
}