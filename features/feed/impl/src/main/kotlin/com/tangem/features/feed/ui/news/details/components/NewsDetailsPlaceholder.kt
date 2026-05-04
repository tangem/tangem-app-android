package com.tangem.features.feed.ui.news.details.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun NewsDetailsPlaceholder(contentPadding: PaddingValues, background: Color, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        NewsDetailsPlaceholderV2(contentPadding, background, modifier)
    } else {
        NewsDetailsPlaceholderV1(background, modifier)
    }
}

@Suppress("LongMethod")
@Composable
private fun NewsDetailsPlaceholderV1(background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp),
    ) {
        RectangleShimmer(modifier = Modifier.size(width = 112.dp, height = 20.dp))
        SpacerH(8.dp)
        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        )
        SpacerH(4.dp)
        RectangleShimmer(modifier = Modifier.size(height = 28.dp, width = 208.dp))
        SpacerH(20.dp)
        RectangleShimmer(modifier = Modifier.size(height = 36.dp, width = 99.dp), radius = 12.dp)
        SpacerH(32.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(end = 30.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(end = 30.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(end = 24.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(end = 70.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(end = 30.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(end = 96.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(end = 100.dp),
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun NewsDetailsPlaceholderV2(contentPadding: PaddingValues, background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp),
    ) {
        SpacerH(contentPadding.calculateTopPadding())
        Row(
            modifier = Modifier.height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Column {
                RectangleShimmer(
                    modifier = Modifier.size(width = 50.dp, height = 20.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                SpacerH(10.dp)
                RectangleShimmer(
                    modifier = Modifier.size(width = 90.dp, height = 18.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }

            VerticalDivider(color = TangemTheme.colors2.border.neutral.primary)

            Column {
                RectangleShimmer(
                    modifier = Modifier.size(width = 50.dp, height = 20.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                SpacerH(10.dp)
                RectangleShimmer(
                    modifier = Modifier.size(width = 90.dp, height = 18.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }
        }

        SpacerH(36.dp)

        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            radius = TangemTheme.dimens2.x25,
        )

        SpacerH(12.dp)

        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(end = 106.dp),
            radius = TangemTheme.dimens2.x25,
        )

        SpacerH(36.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RectangleShimmer(
                modifier = Modifier.size(width = 98.dp, height = 36.dp),
                radius = TangemTheme.dimens2.x25,
            )
            RectangleShimmer(
                modifier = Modifier.size(width = 98.dp, height = 36.dp),
                radius = TangemTheme.dimens2.x25,
            )
        }

        SpacerH(20.dp)

        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(end = 22.dp),
            radius = TangemTheme.dimens2.x25,
        )
        SpacerH(12.dp)
        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(end = 66.dp),
            radius = TangemTheme.dimens2.x25,
        )
        SpacerH(12.dp)
        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(end = 18.dp),
            radius = TangemTheme.dimens2.x25,
        )
        SpacerH(12.dp)
        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(end = 46.dp),
            radius = TangemTheme.dimens2.x25,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewsDetailsPlaceholderPreviewV1() {
    TangemThemePreview {
        NewsDetailsPlaceholder(
            background = TangemTheme.colors.background.tertiary,
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewsDetailsPlaceholderPreviewV2() {
    TangemThemePreviewRedesign {
        NewsDetailsPlaceholder(
            background = TangemTheme.colors2.surface.level3,
            contentPadding = PaddingValues(),
        )
    }
}