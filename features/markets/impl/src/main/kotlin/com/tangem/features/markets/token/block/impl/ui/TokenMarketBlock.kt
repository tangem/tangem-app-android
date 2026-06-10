package com.tangem.features.markets.token.block.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds.row.token.internal.TokenRowPriceChangeContent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.R as CoreR
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.token.block.impl.model.formatter.toChartType
import com.tangem.features.markets.token.block.impl.ui.state.TokenMarketBlockUM
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

private val ChartWidth: Dp = 52.dp
private val ChartHeight: Dp = 32.dp

@Composable
internal fun TokenMarketBlock(tokenMarketBlockUM: TokenMarketBlockUM, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens2.x5))
            .background(TangemTheme.colors2.surface.level3)
            .clickable(onClick = tokenMarketBlockUM.onClick),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.markets_common_market_price),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
        ) {
            Text(
                text = tokenMarketBlockUM.currentPrice.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TangemTheme.typography2.captionMedium12,
                color = TangemTheme.colors2.text.neutral.primary,
            )
            TokenRowPriceChangeContent(
                priceChangeState = PriceChangeState.Content(
                    type = tokenMarketBlockUM.priceChangeType,
                    valueInPercent = tokenMarketBlockUM.h24Percent.orEmpty(),
                ),
                isFlickering = false,
            )
        }

        Box(
            modifier = Modifier.layoutId(TangemRowLayoutId.END_BOTTOM),
        ) {
            val chartModifier = Modifier.requiredSize(width = ChartWidth, height = ChartHeight)

            if (tokenMarketBlockUM.chartData != null) {
                MarketChartMini(
                    rawData = tokenMarketBlockUM.chartData,
                    type = tokenMarketBlockUM.priceChangeType.toChartType(),
                    modifier = chartModifier,
                )
            } else {
                RectangleShimmer(modifier = chartModifier)
            }
        }

        SecondaryTangemButton(
            onClick = tokenMarketBlockUM.onClick,
            tangemIconUM = TangemIconUM.Icon(iconRes = CoreR.drawable.ic_arrow_expand_24),
            shape = TangemButtonShape.Rounded,
            size = TangemButtonSize.X10,
            withHazeEffect = false,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.TAIL)
                .padding(start = TangemTheme.dimens2.x10),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenMarketBlock_Preview(
    @PreviewParameter(TokenMarketBlockPreviewProvider::class) params: TokenMarketBlockUM,
) {
    TangemThemePreviewRedesign {
        TokenMarketBlock(
            tokenMarketBlockUM = params,
            modifier = Modifier.background(TangemTheme.colors2.surface.level3),
        )
    }
}

private class TokenMarketBlockPreviewProvider : PreviewParameterProvider<TokenMarketBlockUM> {

    private val data = MarketChartRawData(
        x = List(size = 20) { Random.nextFloat().toDouble() }.toImmutableList(),
        y = List(size = 20) { Random.nextFloat().toDouble() }.toImmutableList(),
    )

    private val state = TokenMarketBlockUM(
        currencySymbol = "XRP",
        currentPrice = "0,5$",
        currentPriceValue = null,
        priceAnnotated = null,
        h24Percent = "0,5%",
        priceChangeType = PriceChangeType.UP,
        chartData = data,
        onClick = {},
    )

    override val values: Sequence<TokenMarketBlockUM>
        get() = sequenceOf(
            state,
            state.copy(currentPrice = "0,0000000000012356786789$"),
            state.copy(currentPrice = null, chartData = null),
            state.copy(chartData = null),
        )
}
// endregion