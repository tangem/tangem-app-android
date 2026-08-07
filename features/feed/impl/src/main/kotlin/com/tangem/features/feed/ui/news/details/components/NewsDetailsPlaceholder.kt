package com.tangem.features.feed.ui.news.details.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun NewsDetailsPlaceholder(contentPadding: PaddingValues, background: Color, modifier: Modifier = Modifier) {
    NewsDetailsPlaceholderV2(contentPadding, background, modifier)
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
        RectangleShimmer(
            modifier = Modifier.size(width = 90.dp, height = 20.dp),
            radius = TangemTheme.dimens2.x25,
        )

        SpacerH(16.dp)

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

        SpacerH(30.dp)

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
private fun NewsDetailsPlaceholderPreviewV2() {
    TangemThemePreviewRedesign {
        NewsDetailsPlaceholder(
            background = TangemTheme.colors2.surface.level3,
            contentPadding = PaddingValues(),
        )
    }
}