package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
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
            if (LocalRedesignEnabled.current) {
                MostlyUsedItemPlaceholderV2()
            } else {
                MostlyUsedItemPlaceholderV1()
            }
        }
    }
}

@Composable
private fun MostlyUsedItemPlaceholderV1(modifier: Modifier = Modifier) {
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

@Composable
private fun MostlyUsedItemPlaceholderV2(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(178.dp)
            .height(130.dp)
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x4),
            )
            .padding(12.dp),
    ) {
        CircleShimmer(modifier = Modifier.size(40.dp))
        SpacerH(22.dp)
        RectangleShimmer(
            modifier = Modifier
                .width(56.dp)
                .height(20.dp),
            radius = TangemTheme.dimens2.x25,
        )
        SpacerH(4.dp)
        RectangleShimmer(
            modifier = Modifier
                .width(46.dp)
                .height(16.dp),
            radius = TangemTheme.dimens2.x25,
        )
    }
}

private const val PLACEHOLDER_ITEMS_COUNT = 3

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MostlyUsedPlaceholderPreviewV1() {
    TangemThemePreview {
        MostlyUsedPlaceholder()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MostlyUsedPlaceholderPreviewV2() {
    TangemThemePreviewRedesign {
        MostlyUsedPlaceholder()
    }
}