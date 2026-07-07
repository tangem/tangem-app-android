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

private var _ic_control_circle_20: ImageVector? = null

val Icons.ic_control_circle_20: ImageVector
    get() {
        if (_ic_control_circle_20 != null) return _ic_control_circle_20!!
        _ic_control_circle_20 = ImageVector.Builder(
            name = "ic_control_circle_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.25 2C14.8063 2 18.5 5.69365 18.5 10.25C18.5 14.8063 14.8063 18.5 10.25 18.5C5.69365 18.5 2 14.8063 2 10.25C2 5.69365 5.69365 2 10.25 2ZM10.25 3.5C6.52208 3.5 3.5 6.52208 3.5 10.25C3.5 13.9779 6.52208 17 10.25 17C13.9779 17 17 13.9779 17 10.25C17 6.52208 13.9779 3.5 10.25 3.5Z"),
            )
        }.build()
        return _ic_control_circle_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlCircle20Preview() {
    Icon(
        imageVector = Icons.ic_control_circle_20,
        contentDescription = null,
    )
}