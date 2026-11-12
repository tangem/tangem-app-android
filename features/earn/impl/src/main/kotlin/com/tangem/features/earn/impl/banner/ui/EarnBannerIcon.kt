package com.tangem.features.earn.impl.banner.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

internal val EARN_BANNER_ICON_SIZE = 40.dp

@Composable
internal fun EarnBannerIcon(modifier: Modifier = Modifier) {
    val brand = TangemTheme.colors3.icon.brand
    Box(
        modifier = modifier
            .size(EARN_BANNER_ICON_SIZE)
            .drawBehind {
                val radius = 36.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(brand.copy(alpha = 0.3f), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
    ) {
        Bar(start = 6.dp, height = 12.dp, color = brand)
        Bar(start = 17.dp, height = 18.dp, color = brand)
        Bar(start = 28.dp, height = 25.dp, color = brand)
    }
}

@Composable
private fun BoxScope.Bar(start: Dp, height: Dp, color: Color) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = start, y = -6.dp)
            .size(width = 6.dp, height = height)
            .clip(shape)
            .background(color)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.2f)),
                ),
            )
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.15f), shape = shape),
    )
}

// region Preview
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnBannerIconPreview() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(32.dp),
        ) {
            EarnBannerIcon()
        }
    }
}
// endregion