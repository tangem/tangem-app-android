package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun EarnListPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        repeat(PLACEHOLDER_ITEMS_COUNT) {
            EarnItemPlaceholder()
        }
    }
}

@Composable
internal fun EarnItemPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(
            modifier = Modifier.size(36.dp),
        )

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

private const val PLACEHOLDER_ITEMS_COUNT = 8

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListPlaceholderPreview() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            EarnListPlaceholder()
        }
    }
}