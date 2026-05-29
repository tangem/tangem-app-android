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

private var _ic_calendar_16: ImageVector? = null

val Icons.ic_calendar_16: ImageVector
    get() {
        if (_ic_calendar_16 != null) return _ic_calendar_16!!
        _ic_calendar_16 = ImageVector.Builder(
            name = "ic_calendar_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 9.375C10.3452 9.37501 10.625 9.65483 10.625 10C10.625 10.3452 10.3452 10.625 10 10.625H6C5.65484 10.625 5.375 10.3452 5.375 10C5.375 9.65483 5.65484 9.37502 6 9.375H10Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.625 2.375C9.97017 2.37501 10.25 2.65483 10.25 3H10.6016C11.9131 3.00009 12.9764 4.0635 12.9766 5.375V10.6016C12.9766 11.9132 11.9132 12.9765 10.6016 12.9766H5.375C4.06348 12.9764 3 11.9131 3 10.6016V5.375C3.00014 4.06356 4.06357 3.00018 5.375 3H5.75C5.75 2.65484 6.02985 2.37503 6.375 2.375C6.72017 2.37501 7 2.65483 7 3H9C9 2.65484 9.27985 2.37503 9.625 2.375ZM5.375 4.25C4.75392 4.25018 4.25014 4.75391 4.25 5.375V10.6016C4.25 11.2228 4.75383 11.7264 5.375 11.7266H10.6016C11.2228 11.7265 11.7266 11.2228 11.7266 10.6016V5.375C11.7264 4.75386 11.2227 4.25009 10.6016 4.25H10.25C10.25 4.59517 9.97017 4.87499 9.625 4.875C9.27985 4.87497 9 4.59516 9 4.25H7C7 4.59517 6.72017 4.87499 6.375 4.875C6.02985 4.87497 5.75 4.59516 5.75 4.25H5.375Z"),
            )
        }.build()
        return _ic_calendar_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCalendar16Preview() {
    Icon(
        imageVector = Icons.ic_calendar_16,
        contentDescription = null,
    )
}