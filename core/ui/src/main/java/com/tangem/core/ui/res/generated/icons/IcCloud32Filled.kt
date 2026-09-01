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

private var _ic_cloud_32_filled: ImageVector? = null

val Icons.ic_cloud_32_filled: ImageVector
    get() {
        if (_ic_cloud_32_filled != null) return _ic_cloud_32_filled!!
        _ic_cloud_32_filled = ImageVector.Builder(
            name = "ic_cloud_32_filled",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.9995 6C20.4747 6.00014 24.2306 9.20435 24.895 13.4404C27.7946 14.0623 30.0004 16.5807 30.0005 19.6459C30.0003 23.1869 27.0565 25.9998 23.4995 26H9.74951C5.50212 25.9999 1.9997 22.6427 1.99951 18.4304V18.4275L2.0083 18.0729C2.17339 14.7245 4.5746 11.9312 7.82373 11.1251C9.28072 8.01604 12.4714 6.00168 15.9995 6Z"),
            )
        }.build()
        return _ic_cloud_32_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud32FilledPreview() {
    Icon(
        imageVector = Icons.ic_cloud_32_filled,
        contentDescription = null,
    )
}