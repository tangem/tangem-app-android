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

private var _ic_cloud_28: ImageVector? = null

val Icons.ic_cloud_28: ImageVector
    get() {
        if (_ic_cloud_28 != null) return _ic_cloud_28!!
        _ic_cloud_28 = ImageVector.Builder(
            name = "ic_cloud_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 5C17.8391 5.00022 21.0569 7.77074 21.6133 11.4268C24.103 11.9505 25.9988 14.1183 25.999 16.7578C25.9989 19.7961 23.4873 22.2119 20.4492 22.2119H8.625C4.99335 22.2118 2.0001 19.3256 2 15.707V15.7051L2.00781 15.4004C2.14903 12.5177 4.2093 10.1129 6.99609 9.42578C8.23841 6.74209 10.9744 5.00156 14 5ZM14 7.5C11.757 7.50126 9.78673 8.89717 9.07812 10.9355C8.92266 11.3821 8.52842 11.7033 8.05957 11.7646C6.00246 12.034 4.50361 13.7377 4.5 15.71C4.50154 17.8934 6.32116 19.7118 8.625 19.7119H20.4492C22.1604 19.7119 23.4989 18.3622 23.499 16.7578C23.4988 15.1535 22.1604 13.8047 20.4492 13.8047C19.7591 13.8045 19.1993 13.2448 19.1992 12.5547C19.1991 9.87607 17.0397 7.64378 14.2695 7.50684L14 7.5Z"),
            )
        }.build()
        return _ic_cloud_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud28Preview() {
    Icon(
        imageVector = Icons.ic_cloud_28,
        contentDescription = null,
    )
}