package com.tangem.common.ui.markets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.windowsize.WindowSizeType

@Composable
fun MarketsListItemPlaceholder(modifier: Modifier = Modifier) {
    val windowSize = LocalWindowSize.current
    Row(
        modifier = modifier.padding(
            horizontal = 12.dp,
            vertical = 14.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemShimmer(
            radius = 999.dp,
            modifier = Modifier.size(40.dp),
        )

        SpacerW(4.dp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            MarketsListShimmerLine(
                style = TangemTheme.typography3.body.medium,
                width = 96.dp,
            )
            MarketsListShimmerLine(
                style = TangemTheme.typography3.caption.medium,
                width = 46.dp,
            )
        }

        if (windowSize.widthAtLeast(WindowSizeType.Small)) {
            SpacerW(10.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                MarketsListShimmerLine(
                    style = TangemTheme.typography3.body.medium,
                    width = 56.dp,
                )
                MarketsListShimmerLine(
                    style = TangemTheme.typography3.caption.medium,
                    width = 46.dp,
                )
            }
        }
    }
}

@Composable
private fun MarketsListShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Preview(showBackground = true, widthDp = 360, name = "normal")
@Preview(showBackground = true, widthDp = 360, name = "normal night", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, name = "small width")
@Composable
private fun PreviewV1() {
    TangemThemePreview {
        Column(Modifier.background(TangemTheme.colors.background.tertiary)) {
            repeat(20) {
                MarketsListItemPlaceholder()
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, name = "normal")
@Preview(showBackground = true, widthDp = 360, name = "normal night", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, name = "small width")
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign {
        Column(Modifier.background(TangemTheme.colors2.surface.level3)) {
            repeat(20) {
                MarketsListItemPlaceholder()
            }
        }
    }
}