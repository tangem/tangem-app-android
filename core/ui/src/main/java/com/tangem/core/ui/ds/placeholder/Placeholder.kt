package com.tangem.core.ui.ds.placeholder

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Placeholder composable to display a skeleton loading state.
 *
 * @param modifier Modifier to be applied to the placeholder.
 * @param size Size of the placeholder. Default is 100x20 dp.
 * @param radius Corner radius of the placeholder. Default is 25 dp.
 */
@Composable
fun Placeholder(
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(TangemTheme.dimens2.x10, TangemTheme.dimens2.x2),
    radius: Dp = TangemTheme.dimens2.x25,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = TangemTheme.colors2.skeleton.backgroundPrimary,
                shape = RoundedCornerShape(radius),
            ),
    )
}

/**
 * TextPlaceholder composable to display a skeleton loading state for text elements.
 *
 * @param textStyle TextStyle to determine the line height of the placeholder.
 * @param modifier Modifier to be applied to the placeholder.
 * @param width Width of the placeholder. Default is 200 dp.
 * @param radius Corner radius of the placeholder. Default is 25 dp.
 */
@Composable
fun TextPlaceholder(
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    width: Dp = 200.dp,
    radius: Dp = TangemTheme.dimens2.x25,
) {
    val lineHeight = with(LocalDensity.current) { textStyle.lineHeight.toDp() }
    Box(
        modifier = modifier
            .size(width = width, height = lineHeight)
            .background(
                color = TangemTheme.colors2.skeleton.backgroundPrimary,
                shape = RoundedCornerShape(radius),
            ),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Placeholder_Preview() {
    TangemThemePreviewRedesign {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        ) {
            Placeholder()
            TextPlaceholder(
                textStyle = TangemTheme.typography2.titleRegular44,
            )
        }
    }
}
// endregion