package com.tangem.core.ui.ds.topbar

import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

/**
 * User model for top bar action
 *
 * @property iconRes resource id of action icon
 * @property isActionable if true, action will be clickable, otherwise - not
 * @property onClick lambda be invoked when action component is clicked. If null, action will not be clickable
 * @property ghostModeProgress progress of ghost mode animation, from 0f to 1f.
 */
@Immutable
data class TangemTopBarActionUM(
    @param:DrawableRes val iconRes: Int,
    val isActionable: Boolean = true,
    val onClick: (() -> Unit)? = null,
    @param:FloatRange(0.0, 1.0) val ghostModeProgress: Float = 0f,
)