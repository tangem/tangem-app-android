package com.tangem.features.polymarket.impl.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.res.TangemTheme

@Composable
@ReadOnlyComposable
internal fun TangemPriceChange.Direction.polymarketPnlColor(): Color = when (this) {
    TangemPriceChange.Direction.Up -> TangemTheme.colors3.text.accent.blue
    TangemPriceChange.Direction.Down -> TangemTheme.colors3.text.accent.red
    TangemPriceChange.Direction.Neutral -> TangemTheme.colors3.text.tertiary
}