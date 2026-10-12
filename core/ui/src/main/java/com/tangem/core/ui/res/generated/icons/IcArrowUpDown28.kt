@file:Suppress("all")

package com.tangem.core.ui.res.generated.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _ic_arrow_up_down_28: ImageVector? = null

val Icons.ic_arrow_up_down_28: ImageVector
    get() {
        if (_ic_arrow_up_down_28 != null) return _ic_arrow_up_down_28!!
        _ic_arrow_up_down_28 = ImageVector.Builder(
            name = "ic_arrow_up_down_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.95141 4.29297C9.07804 4.29304 9.20272 4.31273 9.32152 4.34961C9.32782 4.35156 9.33383 4.3544 9.34008 4.35645C9.44848 4.39198 9.55159 4.44208 9.64672 4.50586C9.6537 4.51054 9.66035 4.51569 9.66723 4.52051C9.72644 4.56197 9.7834 4.60737 9.8352 4.65918L13.8948 8.7207C14.3827 9.2088 14.3826 10.0002 13.8948 10.4883C13.4066 10.9763 12.6153 10.9763 12.1272 10.4883L10.2014 8.56055V22.459C10.2012 23.1491 9.64158 23.7089 8.95141 23.709C8.26119 23.709 7.70163 23.1492 7.70141 22.459V8.56055L5.77367 10.4883C5.28559 10.9764 4.49427 10.9762 4.00609 10.4883C3.51801 10.0001 3.51796 9.20883 4.00609 8.7207L8.06664 4.65918C8.30106 4.42494 8.62 4.29294 8.95141 4.29297Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.0481 4.29102C19.7383 4.29102 20.2978 4.85088 20.2981 5.54102V19.4414L22.2268 17.5127C22.7149 17.025 23.5063 17.025 23.9944 17.5127C24.4824 18.0007 24.4822 18.7921 23.9944 19.2803L19.9338 23.3418C19.6994 23.5762 19.3806 23.708 19.0491 23.708C18.7176 23.7079 18.3996 23.5762 18.1653 23.3418L14.1057 19.2812C13.6177 18.7931 13.6177 18.0018 14.1057 17.5137C14.5938 17.0257 15.3852 17.0248 15.8733 17.5127L17.7981 19.4385V5.54199C17.7981 4.85166 18.3578 4.29105 19.0481 4.29102Z"),
            )
        }.build()
        return _ic_arrow_up_down_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUpDown28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_down_28,
        contentDescription = null,
    )
}