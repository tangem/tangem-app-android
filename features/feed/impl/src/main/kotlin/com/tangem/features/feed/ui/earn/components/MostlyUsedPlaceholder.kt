package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun MostlyUsedPlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = 16.dp,
        vertical = 12.dp,
    ),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PLACEHOLDER_ITEMS_COUNT) {
            MostlyUsedItemPlaceholder()
        }
    }
}

@Composable
private fun MostlyUsedItemPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(178.dp)
            .height(130.dp)
            .background(
                color = TangemTheme.colors3.bg.secondary,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
    ) {
        TangemShimmer(
            radius = 999.dp,
            modifier = Modifier.size(40.dp),
        )
        SpacerH(22.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MostlyUsedShimmerLine(
                style = TangemTheme.typography3.body.medium,
                width = 56.dp,
                modifier = Modifier.weight(1f, fill = false),
            )
            SpacerW(4.dp)
            MostlyUsedShimmerLine(
                style = TangemTheme.typography3.caption.medium,
                width = 32.dp,
            )
        }
        SpacerH(2.dp)
        MostlyUsedShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 56.dp,
        )
    }
}

@Composable
private fun MostlyUsedShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

private const val PLACEHOLDER_ITEMS_COUNT = 3

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MostlyUsedPlaceholderPreview() {
    TangemThemePreviewRedesign {
        MostlyUsedPlaceholder()
    }
}