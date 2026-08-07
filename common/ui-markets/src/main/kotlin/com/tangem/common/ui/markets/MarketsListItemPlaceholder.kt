package com.tangem.common.ui.markets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.*
import com.tangem.core.ui.windowsize.WindowSizeType

@Suppress("LongMethod")
@Composable
fun MarketsListItemPlaceholder() {
    val windowSize = LocalWindowSize.current
    Row(
        modifier = Modifier.padding(
            horizontal = 12.dp,
            vertical = 14.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(modifier = Modifier.size(40.dp))

        SpacerW(4.dp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            RectangleShimmer(
                modifier = Modifier
                    .width(96.dp)
                    .height(20.dp),
                radius = TangemTheme.dimens2.x25,
            )

            RectangleShimmer(
                modifier = Modifier
                    .width(46.dp)
                    .height(16.dp),
                radius = TangemTheme.dimens2.x25,
            )
        }

        if (windowSize.widthAtLeast(WindowSizeType.Small)) {
            SpacerW(10.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(56.dp)
                        .height(20.dp),
                    radius = TangemTheme.dimens2.x25,
                )

                RectangleShimmer(
                    modifier = Modifier
                        .width(46.dp)
                        .height(16.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }
        }
    }
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