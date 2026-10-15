package com.tangem.core.ui.ds.topbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Type of top bar, affects size and padding of content
 *
 * @see TangemTopBar
 */
enum class TangemTopBarType {
    Default,
    BottomSheet,
    ;

    @ReadOnlyComposable
    @Composable
    fun getSize(): Dp {
        return when (this) {
            Default -> 64.dp
            BottomSheet -> 76.dp
        }
    }

    @ReadOnlyComposable
    @Composable
    fun getSideContentSize(): Dp {
        return 28.dp
    }

    @ReadOnlyComposable
    @Composable
    fun getPadding(): PaddingValues {
        return when (this) {
            Default -> PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            BottomSheet -> PaddingValues(16.dp)
        }
    }
}