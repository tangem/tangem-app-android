package com.tangem.features.polymarket.impl.placeprediction.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PreviewSlippage

@Composable
internal fun PredictionOrderDetailsCard(
    slippage: SlippageUM,
    total: String,
    isTotalLoading: Boolean,
    modifier: Modifier = Modifier,
    onSlippageClick: (() -> Unit)? = null,
    onTotalClick: (() -> Unit)? = null,
) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
        Column {
            PredictionSlippageRow(slippage = slippage, onClick = onSlippageClick)
            PredictionTotalRow(value = total, isLoading = isTotalLoading, onClick = onTotalClick)
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PredictionOrderDetailsCardPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            PredictionOrderDetailsCard(
                slippage = PreviewSlippage,
                total = "$1.40",
                isTotalLoading = false,
                onSlippageClick = {},
                onTotalClick = {},
            )
            PredictionOrderDetailsCard(slippage = PreviewSlippage, total = "", isTotalLoading = true)
        }
    }
}