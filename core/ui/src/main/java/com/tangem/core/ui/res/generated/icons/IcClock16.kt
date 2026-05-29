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

private var _ic_clock_16: ImageVector? = null

val Icons.ic_clock_16: ImageVector
    get() {
        if (_ic_clock_16 != null) return _ic_clock_16!!
        _ic_clock_16 = ImageVector.Builder(
            name = "ic_clock_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.36523 4.5C8.71001 4.50033 8.99003 4.7802 8.99023 5.125V8.26074C8.99023 8.60572 8.71014 8.88542 8.36523 8.88574H6.125C5.77982 8.88574 5.5 8.60592 5.5 8.26074C5.50026 7.91578 5.77998 7.63574 6.125 7.63574H7.74023V5.125C7.74044 4.78 8.02018 4.5 8.36523 4.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M8.30957 2.00781C11.48 2.16874 14.001 4.79058 14.001 8.00098C14.0007 11.3147 11.3146 14.0006 8.00098 14.001C4.68703 14.001 2.00027 11.3149 2 8.00098C2 4.68687 4.68687 2 8.00098 2L8.30957 2.00781ZM8.00098 3.25C5.37722 3.25 3.25 5.37722 3.25 8.00098C3.25027 10.6245 5.37739 12.751 8.00098 12.751C10.6243 12.7506 12.7507 10.6243 12.751 8.00098C12.751 5.37743 10.6244 3.25033 8.00098 3.25Z"),
            )
        }.build()
        return _ic_clock_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClock16Preview() {
    Icon(
        imageVector = Icons.ic_clock_16,
        contentDescription = null,
    )
}