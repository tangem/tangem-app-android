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

private var _ic_cloud_24: ImageVector? = null

val Icons.ic_cloud_24: ImageVector
    get() {
        if (_ic_cloud_24 != null) return _ic_cloud_24!!
        _ic_cloud_24 = ImageVector.Builder(
            name = "ic_cloud_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 4C15.3671 4 18.1808 6.44726 18.6338 9.66113C20.8241 10.0865 22.5 11.9796 22.5 14.2861C22.4998 16.9109 20.3293 18.9999 17.7002 19H7.25C4.09609 19 1.50015 16.4951 1.5 13.3574V13.3555L1.50684 13.0908C1.63028 10.5743 3.4403 8.47551 5.88477 7.89551C6.95542 5.53578 9.34909 4.00127 12 4ZM12 6C9.97439 6.00117 8.19299 7.26207 7.55176 9.10645C7.42736 9.4636 7.11234 9.72049 6.7373 9.76953C4.86865 10.0139 3.50329 11.5622 3.5 13.3594C3.50124 15.3489 5.15808 17 7.25 17H17.7002C19.268 16.9999 20.4998 15.7636 20.5 14.2861C20.5 12.8085 19.2681 11.5714 17.7002 11.5713C17.1479 11.5713 16.7002 11.1236 16.7002 10.5713C16.7001 8.06801 14.6173 6 12 6Z"),
            )
        }.build()
        return _ic_cloud_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud24Preview() {
    Icon(
        imageVector = Icons.ic_cloud_24,
        contentDescription = null,
    )
}