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

private var _ic_cloud_16: ImageVector? = null

val Icons.ic_cloud_16: ImageVector
    get() {
        if (_ic_cloud_16 != null) return _ic_cloud_16!!
        _ic_cloud_16 = ImageVector.Builder(
            name = "ic_cloud_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 3.375C10.1072 3.375 11.8925 4.86911 12.1826 6.8584C13.556 7.11784 14.6248 8.27613 14.625 9.71387C14.625 11.3498 13.2412 12.625 11.5996 12.625H5C3.02705 12.625 1.375 11.0941 1.375 9.14258L1.37891 8.97754C1.45745 7.41255 2.61167 6.12397 4.14746 5.77051C4.82684 4.31304 6.33845 3.37577 8 3.375ZM8 4.625C6.70774 4.62572 5.58437 5.40923 5.18262 6.53516C5.10338 6.75663 4.90621 6.9146 4.67285 6.94434C3.47814 7.09672 2.62729 8.05592 2.625 9.14355C2.6254 10.3476 3.6595 11.375 5 11.375H11.5996C12.609 11.375 13.375 10.6027 13.375 9.71387C13.3748 8.82524 12.6088 8.05371 11.5996 8.05371C11.2547 8.0535 10.9747 7.77369 10.9746 7.42871C10.9746 5.90873 9.67213 4.625 8 4.625Z"),
            )
        }.build()
        return _ic_cloud_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud16Preview() {
    Icon(
        imageVector = Icons.ic_cloud_16,
        contentDescription = null,
    )
}