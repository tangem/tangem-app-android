package com.tangem.features.markets.tokenlist.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.windowsize.WindowSizeType

@Suppress("LongMethod")
@Composable
fun MarketsListItemPlaceholder() {
    val density = LocalDensity.current
    val windowSize = LocalWindowSize.current
    val sp12 = with(density) { 12.sp.toDp() }

    Row(
        modifier = Modifier.padding(
            horizontal = TangemTheme.dimens.spacing16,
            vertical = TangemTheme.dimens.spacing15,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(Modifier.size(TangemTheme.dimens.size36))

        SpacerW12()

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TangemTheme.dimens.spacing4),
                contentAlignment = Alignment.CenterStart,
            ) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(TangemTheme.dimens.size70)
                        .height(sp12),
                    radius = TangemTheme.dimens.radius3,
                )
            }

            SpacerH(height = TangemTheme.dimens.spacing2)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TangemTheme.dimens.spacing2),
                contentAlignment = Alignment.CenterStart,
            ) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(TangemTheme.dimens.size52)
                        .height(sp12),
                    radius = TangemTheme.dimens.radius3,
                )
            }
        }

        if (windowSize.widthAtLeast(WindowSizeType.Small)) {
            Spacer(Modifier.width(TangemTheme.dimens.spacing10))

            Box {
                RectangleShimmer(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(TangemTheme.dimens.size56)
                        .height(TangemTheme.dimens.size12),
                    radius = TangemTheme.dimens.radius3,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, name = "normal")
@Preview(showBackground = true, widthDp = 360, name = "normal night", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, name = "small width")
@Composable
private fun Preview() {
    TangemThemePreview {
        Column(Modifier.background(TangemTheme.colors.background.tertiary)) {
            repeat(20) {
                MarketsListItemPlaceholder()
            }
        }
    }
}
