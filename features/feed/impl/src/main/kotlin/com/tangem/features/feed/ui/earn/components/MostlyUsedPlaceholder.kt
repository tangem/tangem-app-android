package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun MostlyUsedPlaceholder(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PLACEHOLDER_ITEMS_COUNT) {
            MostlyUsedItemPlaceholder()
        }
    }
}

@Composable
fun MostlyUsedItemPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(148.dp)
            .height(102.dp)
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(12.dp),
    ) {
        CircleShimmer(
            modifier = Modifier
                .size(32.dp),
        )
        SpacerH(8.dp)
        RectangleShimmer(
            modifier = Modifier
                .width(74.dp)
                .height(20.dp),
            radius = 4.dp,
        )
        SpacerH(2.dp)
        RectangleShimmer(
            modifier = Modifier
                .width(84.dp)
                .height(16.dp),
            radius = 4.dp,
        )
    }
}

private const val PLACEHOLDER_ITEMS_COUNT = 3

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MostlyUsedPlaceholderPreview() {
    TangemThemePreview {
        MostlyUsedPlaceholder()
    }
}