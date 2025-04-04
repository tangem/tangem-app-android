package com.tangem.features.send.v2.send.ui.state

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

sealed class ButtonsUM {

    data class PrimaryButtonUM(
        val text: TextReference,
        val additionalText: TextReference? = null,
        @DrawableRes val iconResId: Int? = null,
        val isEnabled: Boolean,
        val isHapticClick: Boolean = false,
        val onClick: () -> Unit,
    ) : ButtonsUM()

    data class SecondaryPairButtonsUM(
        val leftText: TextReference,
        @DrawableRes val leftIconResId: Int? = null,
        val onLeftClick: () -> Unit,
        val rightText: TextReference,
        @DrawableRes val rightIconResId: Int? = null,
        val onRightClick: () -> Unit,
    ) : ButtonsUM()
}