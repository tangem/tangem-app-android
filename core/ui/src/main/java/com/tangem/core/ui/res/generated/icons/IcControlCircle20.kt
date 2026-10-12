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
                pathData = addPathNodes("M10 2C14.4183 2 18 5.58172 18 10C18 14.4183 14.4183 18 10 18C5.58172 18 2 14.4183 2 10C2 5.58172 5.58172 2 10 2ZM10 3.45455C6.38505 3.45455 3.45455 6.38505 3.45455 10C3.45455 13.615 6.38505 16.5455 10 16.5455C13.615 16.5455 16.5455 13.615 16.5455 10C16.5455 6.38505 13.615 3.45455 10 3.45455Z"),
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