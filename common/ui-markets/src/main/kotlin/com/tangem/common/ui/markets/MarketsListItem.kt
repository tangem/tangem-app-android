package com.tangem.common.ui.markets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.markets.models.MarketsListItemUM

@Composable
fun MarketsListItem(model: MarketsListItemUM, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    MarketsListItemV2(
        model = model,
        modifier = modifier,
        onClick = onClick,
    )
}