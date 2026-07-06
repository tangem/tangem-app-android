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

private var _ic_globe_28: ImageVector? = null

val Icons.ic_globe_28: ImageVector
    get() {
        if (_ic_globe_28 != null) return _ic_globe_28!!
        _ic_globe_28 = ImageVector.Builder(
            name = "ic_globe_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2.00098C20.6406 2.00098 25.9989 7.35945 25.999 14C25.999 20.6406 20.6406 25.999 14 25.999C7.3595 25.9989 2.00098 20.6405 2.00098 14C2.00106 7.35953 7.35955 2.00111 14 2.00098ZM4.58496 15.25C5.07162 18.9779 7.70365 22.0139 11.21 23.084C9.68187 20.704 8.81707 17.9988 8.61621 15.25H4.58496ZM19.3838 15.25C19.1829 17.9989 18.3182 20.704 16.79 23.084C20.2966 22.014 22.9294 18.978 23.416 15.25H19.3838ZM11.124 15.25C11.3522 17.9124 12.3104 20.5186 14 22.709C15.6897 20.5185 16.6488 17.9125 16.877 15.25H11.124ZM11.21 4.91504C7.70357 5.98504 5.07174 9.02213 4.58496 12.75H8.61621C8.81713 10.0009 9.68156 7.29511 11.21 4.91504ZM14 5.29004C12.3101 7.48053 11.3523 10.0874 11.124 12.75H16.877C16.6487 10.0873 15.69 7.48058 14 5.29004ZM16.79 4.91504C18.3185 7.29515 19.1829 10.0009 19.3838 12.75H23.416C22.9292 9.02196 20.2966 5.98494 16.79 4.91504Z"),
            )
        }.build()
        return _ic_globe_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGlobe28Preview() {
    Icon(
        imageVector = Icons.ic_globe_28,
        contentDescription = null,
    )
}