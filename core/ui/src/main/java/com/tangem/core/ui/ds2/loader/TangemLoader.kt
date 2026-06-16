package com.tangem.core.ui.ds2.loader

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*

/**
 * Loader DS component.
 *
 * Indeterminate circular spinner that rotates continuously to indicate ongoing work.
 *
 * Version: 1.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=25-5688&m=device-tdp-1&t=9n7s8Xo2l3mLh5j-4)
 *
 * @param modifier modifier applied to the loader's root.
 * @param color tint applied to the spinner asset. Defaults to the primary icon color from the
 * current theme.
 * @param size visual size of the spinner; selects both the icon dimensions and the matching
 * pre-rendered spinner asset. Defaults to [TangemLoaderSize.X24].
 */
@Composable
fun TangemLoader(
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors3.icon.primary,
    size: TangemLoaderSize = TangemLoaderSize.X24,
) {
    val transition = rememberInfiniteTransition(label = "TangemLoaderRotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "TangemLoaderRotationAngle",
    )

    Icon(
        modifier = modifier
            .progressSemantics()
            .size(size.sizeDp)
            .graphicsLayer { rotationZ = rotation },
        imageVector = size.imageVector,
        tint = color,
        contentDescription = null,
    )
}

/**
 * Size variants for [TangemLoader]. Each entry pairs a fixed pixel size with a
 * pre-rendered spinner asset of the matching dimensions.
 *
 * @property sizeDp side length applied to the loader via `Modifier.size(...)`.
 * @property imageVector pre-rendered spinner asset matching [sizeDp]; rotated at runtime to animate.
 */
enum class TangemLoaderSize(
    internal val sizeDp: Dp,
    internal val imageVector: ImageVector,
) {
    X12(sizeDp = 12.dp, imageVector = Icons.ic_loading_spinner_12),
    X16(sizeDp = 16.dp, imageVector = Icons.ic_loading_spinner_16),
    X20(sizeDp = 20.dp, imageVector = Icons.ic_loading_spinner_20),
    X24(sizeDp = 24.dp, imageVector = Icons.ic_loading_spinner_24),
    X28(sizeDp = 28.dp, imageVector = Icons.ic_loading_spinner_28),
    X32(sizeDp = 32.dp, imageVector = Icons.ic_loading_spinner_32),
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemLoader_Preview() {
    TangemThemePreviewRedesign {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(12.dp),
        ) {
            TangemLoaderSize.entries.forEach { size ->
                TangemLoader(size = size)
            }
        }
    }
}