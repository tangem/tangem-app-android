package com.tangem.core.ui.components.buttons.common

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
sealed interface TangemButtonIconPosition {
    val iconResId: Int?
    val iconTint: Color?

    data class Start(
        @DrawableRes override val iconResId: Int,
        override val iconTint: Color? = null,
    ) : TangemButtonIconPosition

    data class End(
        @DrawableRes override val iconResId: Int,
        override val iconTint: Color? = null,
    ) : TangemButtonIconPosition

    data object None : TangemButtonIconPosition {
        @DrawableRes
        override val iconResId: Int? = null

        override val iconTint: Color? = null
    }
}