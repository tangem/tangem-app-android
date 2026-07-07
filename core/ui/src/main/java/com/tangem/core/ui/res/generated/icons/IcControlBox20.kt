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

private var _ic_control_box_20: ImageVector? = null

val Icons.ic_control_box_20: ImageVector
    get() {
        if (_ic_control_box_20 != null) return _ic_control_box_20!!
        _ic_control_box_20 = ImageVector.Builder(
            name = "ic_control_box_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.75 3C15.5449 3 17 4.45507 17 6.25V13.75C17 15.5449 15.5449 17 13.75 17H6.25C4.45507 17 3 15.5449 3 13.75V6.25C3 4.45508 4.45508 3 6.25 3H13.75ZM6.25 4.5C5.2835 4.5 4.5 5.2835 4.5 6.25V13.75C4.5 14.7165 5.2835 15.5 6.25 15.5H13.75C14.7165 15.5 15.5 14.7165 15.5 13.75V6.25C15.5 5.2835 14.7165 4.5 13.75 4.5H6.25Z"),
            )
        }.build()
        return _ic_control_box_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlBox20Preview() {
    Icon(
        imageVector = Icons.ic_control_box_20,
        contentDescription = null,
    )
}