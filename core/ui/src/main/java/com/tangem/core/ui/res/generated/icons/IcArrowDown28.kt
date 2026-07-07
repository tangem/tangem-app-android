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

private var _ic_arrow_down_28: ImageVector? = null

val Icons.ic_arrow_down_28: ImageVector
    get() {
        if (_ic_arrow_down_28 != null) return _ic_arrow_down_28!!
        _ic_arrow_down_28 = ImageVector.Builder(
            name = "ic_arrow_down_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.35954 16.751C3.87516 16.2595 3.88101 15.468 4.37224 14.9834C4.86381 14.4991 5.65524 14.5048 6.13981 14.9961L12.7511 21.7021V3.25C12.7513 2.5599 13.3111 2.00024 14.0011 2C14.6911 2.00033 15.251 2.55995 15.2511 3.25V21.7012L21.8615 14.9961C22.3461 14.5048 23.1385 14.499 23.63 14.9834C24.1212 15.468 24.1269 16.2594 23.6427 16.751L14.8908 25.6279C14.6561 25.8657 14.3353 25.9998 14.0011 26C13.6668 25.9999 13.3453 25.866 13.1105 25.6279L4.35954 16.751Z"),
            )
        }.build()
        return _ic_arrow_down_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_28,
        contentDescription = null,
    )
}