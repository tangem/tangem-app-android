package com.tangem.core.ui.ds.topbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.res.TangemTheme

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
            Default -> TangemTheme.dimens2.x16
            BottomSheet -> TangemTheme.dimens2.x19
        }
    }

    @ReadOnlyComposable
    @Composable
    fun getSideContentSize(): Dp {
        return when (this) {
            Default -> TangemTheme.dimens2.x8
            BottomSheet -> TangemTheme.dimens2.x7
        }
    }

    @ReadOnlyComposable
    @Composable
    fun getPadding(): PaddingValues {
        return when (this) {
            Default -> PaddingValues(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3)
            BottomSheet -> PaddingValues(TangemTheme.dimens2.x4)
        }
    }
}