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

private var _ic_cloud_28_filled: ImageVector? = null

val Icons.ic_cloud_28_filled: ImageVector
    get() {
        if (_ic_cloud_28_filled != null) return _ic_cloud_28_filled!!
        _ic_cloud_28_filled = ImageVector.Builder(
            name = "ic_cloud_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0005 5C17.8396 5.00022 21.0574 7.77074 21.6138 11.4268C24.1034 11.9505 25.9993 14.1183 25.9995 16.7578C25.9994 19.7961 23.4878 22.2119 20.4497 22.2119H8.62549C4.99384 22.2118 2.00058 19.3256 2.00049 15.707V15.7051L2.0083 15.4004C2.14952 12.5177 4.20978 10.1129 6.99658 9.42578C8.2389 6.74209 10.9749 5.00156 14.0005 5Z"),
            )
        }.build()
        return _ic_cloud_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud28FilledPreview() {
    Icon(
        imageVector = Icons.ic_cloud_28_filled,
        contentDescription = null,
    )
}