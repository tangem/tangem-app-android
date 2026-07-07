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

private var _ic_calendar_20: ImageVector? = null

val Icons.ic_calendar_20: ImageVector
    get() {
        if (_ic_calendar_20 != null) return _ic_calendar_20!!
        _ic_calendar_20 = ImageVector.Builder(
            name = "ic_calendar_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.252 13C13.6662 13 14.002 13.3358 14.002 13.75C14.0019 14.1642 13.6661 14.5 13.252 14.5H6.74609C6.33192 14.5 5.99614 14.1642 5.99609 13.75C5.99609 13.3358 6.33189 13 6.74609 13H13.252Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.25 2.5C13.6642 2.50001 14 2.83579 14 3.25V3.5H14.2324C15.7512 3.50001 16.9824 4.73122 16.9824 6.25V14.2344C16.9823 15.7531 15.7511 16.9844 14.2324 16.9844H5.76758C4.24897 16.9842 3.01767 15.753 3.01758 14.2344V6.25C3.01758 4.73131 4.24892 3.50014 5.76758 3.5H6V3.25293C6 2.83872 6.3358 2.50294 6.75 2.50293C7.1642 2.50294 7.5 2.83872 7.5 3.25293V3.5H9.25V3.25C9.25 2.83579 9.58579 2.50001 10 2.5C10.4142 2.50001 10.75 2.83579 10.75 3.25V3.5H12.5V3.25C12.5 2.83579 12.8358 2.50001 13.25 2.5ZM5.76758 5C5.07735 5.00014 4.51758 5.55973 4.51758 6.25V14.2344C4.51767 14.9246 5.0774 15.4842 5.76758 15.4844H14.2324C14.9227 15.4844 15.4823 14.9246 15.4824 14.2344V6.25C15.4824 5.55965 14.9228 5.00001 14.2324 5H14V5.25C14 5.66417 13.6642 5.99999 13.25 6C12.8358 5.99999 12.5 5.66417 12.5 5.25V5H10.75V5.25C10.75 5.66417 10.4142 5.99999 10 6C9.58582 5.99999 9.25004 5.66417 9.25 5.25V5H7.5V5.25293C7.49962 5.66681 7.16397 6.00292 6.75 6.00293C6.33603 6.00292 6.00038 5.66681 6 5.25293V5H5.76758Z"),
            )
        }.build()
        return _ic_calendar_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCalendar20Preview() {
    Icon(
        imageVector = Icons.ic_calendar_20,
        contentDescription = null,
    )
}