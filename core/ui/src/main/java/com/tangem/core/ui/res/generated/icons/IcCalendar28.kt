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

private var _ic_calendar_28: ImageVector? = null

val Icons.ic_calendar_28: ImageVector
    get() {
        if (_ic_calendar_28 != null) return _ic_calendar_28!!
        _ic_calendar_28 = ImageVector.Builder(
            name = "ic_calendar_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.7471 18.5C19.4371 18.5003 19.9969 19.0599 19.9971 19.75C19.9971 20.4402 19.4372 20.9997 18.7471 21H9.25098C8.56063 21 8.00098 20.4403 8.00098 19.75C8.00111 19.0598 8.56071 18.5 9.25098 18.5H18.7471Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.75 2.5C19.4403 2.5 19.9999 3.05976 20 3.75V4.25H20.748C23.0952 4.25002 24.998 6.1528 24.998 8.5V20.7471C24.9979 23.0941 23.0952 24.9971 20.748 24.9971H7.25C4.90289 24.9971 3.00015 23.0941 3 20.7471V8.5C3 6.1528 4.9028 4.25001 7.25 4.25H8V3.75C8.00013 3.05976 8.55973 2.50001 9.25 2.5C9.94027 2.5 10.4999 3.05976 10.5 3.75V4.25H12.75V3.75C12.75 3.05964 13.3096 2.5 14 2.5C14.6903 2.50007 15.25 3.05969 15.25 3.75V4.25H17.5V3.75C17.5001 3.05976 18.0597 2.50001 18.75 2.5ZM7.25 6.75C6.28351 6.75001 5.5 7.53351 5.5 8.5V20.7471C5.50015 21.7134 6.2836 22.4971 7.25 22.4971H20.748C21.7144 22.4971 22.4979 21.7134 22.498 20.7471V8.5C22.498 7.53351 21.7145 6.75002 20.748 6.75H20V7.25C20 7.94036 19.4404 8.5 18.75 8.5C18.0597 8.49999 17.5 7.94035 17.5 7.25V6.75H15.25V7.25C15.2497 7.94008 14.6901 8.49993 14 8.5C13.3098 8.5 12.7503 7.94013 12.75 7.25V6.75H10.5V7.25C10.5 7.94036 9.94036 8.5 9.25 8.5C8.55965 8.49999 8 7.94035 8 7.25V6.75H7.25Z"),
            )
        }.build()
        return _ic_calendar_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCalendar28Preview() {
    Icon(
        imageVector = Icons.ic_calendar_28,
        contentDescription = null,
    )
}