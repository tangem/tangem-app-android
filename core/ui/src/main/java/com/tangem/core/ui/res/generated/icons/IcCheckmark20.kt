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

private var _ic_checkmark_20: ImageVector? = null

val Icons.ic_checkmark_20: ImageVector
    get() {
        if (_ic_checkmark_20 != null) return _ic_checkmark_20!!
        _ic_checkmark_20 = ImageVector.Builder(
            name = "ic_checkmark_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.459 4.48048C16.7459 4.18175 17.2208 4.17217 17.5195 4.459C17.8183 4.74588 17.8279 5.22077 17.541 5.51955L7.93947 15.5195C7.79806 15.6668 7.60259 15.75 7.39845 15.75C7.19434 15.75 6.99882 15.6668 6.85744 15.5195L2.459 10.9395C2.17214 10.6407 2.18178 10.1658 2.48048 9.87892C2.77923 9.59202 3.25412 9.60168 3.54103 9.90041L7.39748 13.917L16.459 4.48048Z"),
            )
        }.build()
        return _ic_checkmark_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCheckmark20Preview() {
    Icon(
        imageVector = Icons.ic_checkmark_20,
        contentDescription = null,
    )
}