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

private var _ic_cloud_20: ImageVector? = null

val Icons.ic_cloud_20: ImageVector
    get() {
        if (_ic_cloud_20 != null) return _ic_cloud_20!!
        _ic_cloud_20 = ImageVector.Builder(
            name = "ic_cloud_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 3.75C12.8178 3.75 15.1611 5.82435 15.5049 8.52734C17.3416 8.85425 18.75 10.4345 18.75 12.3574C18.7498 14.5199 16.9684 16.25 14.7998 16.25H6C3.38944 16.25 1.25 14.1679 1.25 11.5713L1.25586 11.3516C1.35816 9.25318 2.87061 7.49898 4.91504 7.03223C5.79136 5.04783 7.78557 3.75106 10 3.75ZM10 5.25C8.25848 5.25099 6.7222 6.34001 6.16797 7.94141C6.07509 8.20984 5.83827 8.40343 5.55664 8.44043C3.94147 8.65271 2.75286 9.99978 2.75 11.5732C2.751 13.3152 4.1929 14.75 6 14.75H14.7998C16.1656 14.75 17.2498 13.6662 17.25 12.3574C17.25 11.0486 16.1657 9.96387 14.7998 9.96387C14.3857 9.96376 14.0498 9.62802 14.0498 9.21387C14.0496 7.03732 12.2494 5.25 10 5.25Z"),
            )
        }.build()
        return _ic_cloud_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud20Preview() {
    Icon(
        imageVector = Icons.ic_cloud_20,
        contentDescription = null,
    )
}