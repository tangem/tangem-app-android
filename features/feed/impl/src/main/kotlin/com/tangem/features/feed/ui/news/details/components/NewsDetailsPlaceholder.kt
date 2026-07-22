package com.tangem.features.feed.ui.news.details.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun NewsDetailsPlaceholder(contentPadding: PaddingValues, background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp),
    ) {
        SpacerH(contentPadding.calculateTopPadding())
        NewsDetailsShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 90.dp,
        )

        SpacerH(16.dp)

        NewsDetailsShimmerLine(
            style = TangemTheme.typography3.heading.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        SpacerH(12.dp)

        NewsDetailsShimmerLine(
            style = TangemTheme.typography3.heading.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 106.dp),
        )

        SpacerH(30.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NewsDetailsTagShimmer(width = 98.dp)
            NewsDetailsTagShimmer(width = 98.dp)
        }

        SpacerH(20.dp)

        NewsDetailsShimmerLine(
            style = TangemTheme.typography3.body.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 22.dp),
        )
        SpacerH(12.dp)
        NewsDetailsShimmerLine(
            style = TangemTheme.typography3.body.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 66.dp),
        )
        SpacerH(12.dp)
        NewsDetailsShimmerLine(
            style = TangemTheme.typography3.body.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 18.dp),
        )
        SpacerH(12.dp)
        NewsDetailsShimmerLine(
            style = TangemTheme.typography3.body.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 46.dp),
        )
    }
}

@Composable
private fun NewsDetailsShimmerLine(style: TextStyle, modifier: Modifier = Modifier, width: Dp? = null) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun NewsDetailsTagShimmer(width: Dp, modifier: Modifier = Modifier) {
    TangemShimmer(
        radius = 999.dp,
        modifier = modifier.size(width = width, height = 36.dp),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewsDetailsPlaceholderPreview() {
    TangemThemePreviewRedesign {
        NewsDetailsPlaceholder(
            background = TangemTheme.colors3.bg.secondary,
            contentPadding = PaddingValues(),
        )
    }
}