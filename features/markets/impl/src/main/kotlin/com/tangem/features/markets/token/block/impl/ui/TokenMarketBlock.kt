package com.tangem.features.markets.token.block.impl.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun TokenMarketBlock(modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        title = {
            Text(
                text = "Token Market Block",
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
    ) {
        Text("//TODO")
    }
}