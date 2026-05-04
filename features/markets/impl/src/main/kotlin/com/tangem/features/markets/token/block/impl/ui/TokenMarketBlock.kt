package com.tangem.features.markets.token.block.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.token.block.impl.ui.state.TokenMarketBlockUM

@Suppress("UnusedParameter")
@Composable
internal fun TokenMarketBlock(tokenMarketBlockUM: TokenMarketBlockUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Market Block Redesign",
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}