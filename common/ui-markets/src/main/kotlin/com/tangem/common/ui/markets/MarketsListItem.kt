package com.tangem.common.ui.markets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.res.LocalRedesignEnabled

@Composable
fun MarketsListItem(model: MarketsListItemUM, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    if (LocalRedesignEnabled.current) {
        MarketsListItemV2(
            model = model,
            modifier = modifier,
            onClick = onClick,
        )
    } else {
        MarketsListItemV1(
            model = model,
            modifier = modifier,
            onClick = onClick,
        )
    }
}