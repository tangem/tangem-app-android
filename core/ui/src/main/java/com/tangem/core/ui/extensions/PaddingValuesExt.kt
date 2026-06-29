package com.tangem.core.ui.extensions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

/**
 * Returns a copy of this [PaddingValues] with the given edges overridden, leaving the rest unchanged.
 */
@Composable
fun PaddingValues.copy(start: Dp? = null, top: Dp? = null, end: Dp? = null, bottom: Dp? = null): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = start ?: calculateStartPadding(layoutDirection),
        top = top ?: calculateTopPadding(),
        end = end ?: calculateEndPadding(layoutDirection),
        bottom = bottom ?: calculateBottomPadding(),
    )
}