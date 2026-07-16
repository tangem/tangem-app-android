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

private var _ic_calendar_24: ImageVector? = null

val Icons.ic_calendar_24: ImageVector
    get() {
        if (_ic_calendar_24 != null) return _ic_calendar_24!!
        _ic_calendar_24 = ImageVector.Builder(
            name = "ic_calendar_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 16C16.5523 16 17 16.4477 17 17C17 17.5523 16.5523 18 16 18H8C7.44772 18 7 17.5523 7 17C7 16.4477 7.44772 16 8 16H16Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2.49414C12.5523 2.49414 13 2.94186 13 3.49414V4H15V3.5C15 2.94772 15.4477 2.50001 16 2.5C16.5523 2.5 17 2.94772 17 3.5V4H17.5107C19.4437 4 21.0107 5.56705 21.0107 7.5V17.502C21.0107 19.4349 19.4437 21.002 17.5107 21.002H6.48926C4.55642 21.0018 2.98927 19.4348 2.98926 17.502V7.5C2.98931 5.56716 4.55645 4.00018 6.48926 4H7V3.5C7 2.94772 7.44773 2.50001 8 2.5C8.55228 2.5 9 2.94772 9 3.5V4H11V3.49414C11 2.94186 11.4477 2.49415 12 2.49414ZM6.48926 6C5.66102 6.00018 4.98931 6.67173 4.98926 7.5V17.502C4.98927 18.3303 5.66099 19.0018 6.48926 19.002H17.5107C18.3392 19.002 19.0107 18.3304 19.0107 17.502V7.5C19.0107 6.67162 18.3391 6 17.5107 6H17V6.5C17 7.05228 16.5523 7.5 16 7.5C15.4477 7.49999 15 7.05228 15 6.5V6H13V6.5C13 7.05228 12.5523 7.5 12 7.5C11.4477 7.49999 11 7.05228 11 6.5V6H9V6.5C9 7.05228 8.55228 7.5 8 7.5C7.44773 7.49999 7 7.05228 7 6.5V6H6.48926Z"),
            )
        }.build()
        return _ic_calendar_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCalendar24Preview() {
    Icon(
        imageVector = Icons.ic_calendar_24,
        contentDescription = null,
    )
}