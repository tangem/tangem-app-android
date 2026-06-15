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

private var _ic_cloud_12: ImageVector? = null

val Icons.ic_cloud_12: ImageVector
    get() {
        if (_ic_cloud_12 != null) return _ic_cloud_12!!
        _ic_cloud_12 = ImageVector.Builder(
            name = "ic_cloud_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 2.5C7.58676 2.5 8.93378 3.61968 9.16406 5.11621C10.1976 5.32125 11 6.19869 11 7.28613C10.9998 8.53149 9.94696 9.4999 8.7002 9.5H3.75C2.25458 9.5 1.00016 8.33961 1 6.85742L1.0127 6.6084C1.12395 5.48086 1.97219 4.56517 3.08887 4.30176C3.60666 3.20468 4.74729 2.50058 6 2.5ZM6 3.5C5.0434 3.50054 4.21375 4.08011 3.91699 4.91113C3.8537 5.08852 3.69562 5.21546 3.50879 5.23926C2.62693 5.35165 2.0015 6.05933 2 6.8584C2.00072 7.74248 2.7607 8.5 3.75 8.5H8.7002C9.44118 8.4999 9.99976 7.93382 10 7.28613C10 6.63827 9.44134 6.07139 8.7002 6.07129C8.42405 6.07129 8.2002 5.84743 8.2002 5.57129C8.20012 4.45004 7.23824 3.5 6 3.5Z"),
            )
        }.build()
        return _ic_cloud_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud12Preview() {
    Icon(
        imageVector = Icons.ic_cloud_12,
        contentDescription = null,
    )
}