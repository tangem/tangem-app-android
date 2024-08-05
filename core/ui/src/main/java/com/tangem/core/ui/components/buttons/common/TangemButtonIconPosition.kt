package com.tangem.core.ui.components.buttons.common

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
sealed interface TangemButtonIconPosition {
    val iconResId: Int?

    data class Start(@DrawableRes override val iconResId: Int) : TangemButtonIconPosition

    data class End(@DrawableRes override val iconResId: Int) : TangemButtonIconPosition

    data object None : TangemButtonIconPosition {
        @DrawableRes
        override val iconResId: Int? = null
    }
}
