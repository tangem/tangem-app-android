package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun EarnItemPlaceholderV1(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(modifier = Modifier.size(36.dp))

        SpacerW(12.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(70.dp)
                        .height(12.dp),
                    radius = 4.dp,
                )
                SpacerWMax()
                RectangleShimmer(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp),
                    radius = 4.dp,
                )
            }

            SpacerH(8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(52.dp)
                        .height(12.dp),
                    radius = 4.dp,
                )
                SpacerWMax()
                RectangleShimmer(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp),
                    radius = 4.dp,
                )
            }
        }
    }
}

@Composable
internal fun EarnItemPlaceholderV2(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(modifier = Modifier.size(40.dp))

        SpacerW(4.dp)

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(96.dp)
                        .height(20.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                SpacerWMax()
                RectangleShimmer(
                    modifier = Modifier
                        .width(56.dp)
                        .height(20.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }

            SpacerH(4.dp)

            Row(modifier = Modifier.fillMaxWidth()) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(46.dp)
                        .height(16.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                SpacerWMax()
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

private const val PLACEHOLDER_ITEMS_COUNT = 8

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListPlaceholderPreviewV1() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            repeat(PLACEHOLDER_ITEMS_COUNT) {
                EarnItemPlaceholderV1()
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListPlaceholderPreviewV2() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level3),
        ) {
            repeat(PLACEHOLDER_ITEMS_COUNT) {
                EarnItemPlaceholderV2()
            }
        }
    }
}