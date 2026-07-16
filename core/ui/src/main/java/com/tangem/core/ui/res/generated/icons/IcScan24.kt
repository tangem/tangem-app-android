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

private var _ic_scan_24: ImageVector? = null

val Icons.ic_scan_24: ImageVector
    get() {
        if (_ic_scan_24 != null) return _ic_scan_24!!
        _ic_scan_24 = ImageVector.Builder(
            name = "ic_scan_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.5 11.5C22.0523 11.5 22.5 11.9477 22.5 12.5C22.5 13.0523 22.0523 13.5 21.5 13.5H21V16C21 18.7614 18.7614 21 16 21H15.5C14.9477 21 14.5 20.5523 14.5 20C14.5 19.4477 14.9477 19 15.5 19H16C17.6569 19 19 17.6569 19 16V13.5H5V16C5 17.6569 6.34315 19 8 19H8.5C9.05228 19 9.5 19.4477 9.5 20C9.5 20.5523 9.05228 21 8.5 21H8C5.23858 21 3 18.7614 3 16V13.5H2.5C1.94772 13.5 1.5 13.0523 1.5 12.5C1.5 11.9477 1.94772 11.5 2.5 11.5H21.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.5 3C9.05228 3 9.5 3.44772 9.5 4C9.5 4.55228 9.05228 5 8.5 5H8C6.34315 5 5 6.34315 5 8V8.5C5 9.05228 4.55228 9.5 4 9.5C3.44772 9.5 3 9.05228 3 8.5V8C3 5.23858 5.23858 3 8 3H8.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 3C18.7614 3 21 5.23858 21 8V8.5C21 9.05228 20.5523 9.5 20 9.5C19.4477 9.5 19 9.05228 19 8.5V8C19 6.34315 17.6569 5 16 5H15.5C14.9477 5 14.5 4.55228 14.5 4C14.5 3.44772 14.9477 3 15.5 3H16Z"),
            )
        }.build()
        return _ic_scan_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcScan24Preview() {
    Icon(
        imageVector = Icons.ic_scan_24,
        contentDescription = null,
    )
}