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

private var _ic_arrow_down_20: ImageVector? = null

val Icons.ic_arrow_down_20: ImageVector
    get() {
        if (_ic_arrow_down_20 != null) return _ic_arrow_down_20!!
        _ic_arrow_down_20 = ImageVector.Builder(
            name = "ic_arrow_down_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.25031 3.33331L9.2503 15.2728L3.86359 9.88605C3.57073 9.59337 3.09589 9.59337 2.80304 9.88605C2.5102 10.1789 2.51031 10.6537 2.80304 10.9466L9.47003 17.6136L9.58429 17.7073C9.70654 17.7888 9.85124 17.8333 10.0003 17.8333C10.1991 17.8332 10.39 17.7542 10.5306 17.6136L17.1966 10.9466C17.4895 10.6537 17.4895 10.1789 17.1966 9.88605C16.9037 9.59348 16.4288 9.59326 16.136 9.88605L10.7503 15.2728L10.7503 3.33331C10.7503 2.91921 10.4144 2.58349 10.0003 2.58331C9.58609 2.58331 9.25031 2.9191 9.25031 3.33331Z"),
            )
        }.build()
        return _ic_arrow_down_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_20,
        contentDescription = null,
    )
}