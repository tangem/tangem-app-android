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

private var _ic_box_20: ImageVector? = null

val Icons.ic_box_20: ImageVector
    get() {
        if (_ic_box_20 != null) return _ic_box_20!!
        _ic_box_20 = ImageVector.Builder(
            name = "ic_box_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.9209 2.29877C9.54591 1.92551 10.3098 1.90189 10.9521 2.22846L11.0791 2.29877L15.9395 5.20405C16.2607 5.39594 16.519 5.66521 16.7002 5.97846C16.7026 5.98232 16.7057 5.98626 16.708 5.99018C16.7219 6.01346 16.734 6.03739 16.7451 6.06147C16.9094 6.3758 17 6.72942 17 7.0937V12.9062C17 13.6755 16.6043 14.3993 15.9404 14.7958L11.0791 17.7011C10.4541 18.0744 9.69021 18.098 9.04785 17.7714L8.9209 17.7011L4.05957 14.7958C3.39651 14.3988 3.00002 13.6747 3 12.9052V7.0937C3.00003 6.32441 3.3957 5.60057 4.05957 5.20405L8.9209 2.29877ZM4.5 12.9052C4.50002 13.164 4.63345 13.39 4.83008 13.5078L9.25 16.1503V10.4248L4.5 7.58491V12.9052ZM10.75 10.4248V16.1503L15.1709 13.5078L15.2412 13.4589C15.3979 13.3339 15.5 13.1328 15.5 12.9062V7.58588L10.75 10.4248ZM5.21094 6.26264L10 9.12592L11.6631 8.13081L6.87402 5.2685L5.21094 6.26264ZM10.3096 3.58588C10.1167 3.4709 9.88328 3.47171 9.69043 3.58686L9.68945 3.58588L8.33594 4.39448L13.125 7.25776L14.7881 6.26362L10.3096 3.58588Z"),
            )
        }.build()
        return _ic_box_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox20Preview() {
    Icon(
        imageVector = Icons.ic_box_20,
        contentDescription = null,
    )
}