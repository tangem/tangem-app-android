package com.tangem.core.ui.ds.row.internal

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * Tail UI model for row components
 */
@Immutable
sealed class TangemRowTailUM {
    data class Text(
        val text: TextReference,
    ) : TangemRowTailUM()

    data class Icon(
        @DrawableRes val iconRes: Int,
    ) : TangemRowTailUM()

    data class Draggable(
        @DrawableRes val iconRes: Int,
    ) : TangemRowTailUM()

    data object Empty : TangemRowTailUM()
}