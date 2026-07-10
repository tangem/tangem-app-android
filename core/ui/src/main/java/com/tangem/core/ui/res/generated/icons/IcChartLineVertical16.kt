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

private var _ic_chart_line_vertical_16: ImageVector? = null

val Icons.ic_chart_line_vertical_16: ImageVector
    get() {
        if (_ic_chart_line_vertical_16 != null) return _ic_chart_line_vertical_16!!
        _ic_chart_line_vertical_16 = ImageVector.Builder(
            name = "ic_chart_line_vertical_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.00098 9.59766C4.34573 9.59816 4.62598 9.87779 4.62598 10.2227V12C4.62578 12.3447 4.34561 12.6245 4.00098 12.625C3.65592 12.625 3.3752 12.345 3.375 12V10.2227C3.375 9.87748 3.6558 9.59766 4.00098 9.59766Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.66699 7.81934C7.01193 7.81957 7.29193 8.09936 7.29199 8.44434V12C7.29192 12.345 7.01193 12.6248 6.66699 12.625C6.32186 12.625 6.04206 12.3451 6.04199 12V8.44434C6.04206 8.09921 6.32185 7.81934 6.66699 7.81934Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.33398 5.59766C9.67896 5.59789 9.95898 5.87762 9.95898 6.22266V12C9.95879 12.3449 9.67884 12.6248 9.33398 12.625C8.98893 12.625 8.70918 12.345 8.70898 12V6.22266C8.70898 5.87748 8.98881 5.59766 9.33398 5.59766Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.001 3.375C12.3457 3.3755 12.626 3.65513 12.626 4V12C12.626 12.3449 12.3457 12.6245 12.001 12.625C11.6558 12.625 11.376 12.3452 11.376 12V4C11.376 3.65482 11.6558 3.375 12.001 3.375Z"),
            )
        }.build()
        return _ic_chart_line_vertical_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartLineVertical16Preview() {
    Icon(
        imageVector = Icons.ic_chart_line_vertical_16,
        contentDescription = null,
    )
}