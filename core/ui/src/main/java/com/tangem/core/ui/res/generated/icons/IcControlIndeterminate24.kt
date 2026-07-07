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

private var _ic_control_indeterminate_24: ImageVector? = null

val Icons.ic_control_indeterminate_24: ImageVector
    get() {
        if (_ic_control_indeterminate_24 != null) return _ic_control_indeterminate_24!!
        _ic_control_indeterminate_24 = ImageVector.Builder(
            name = "ic_control_indeterminate_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 11C16.5523 11 17 11.4477 17 12C17 12.5523 16.5523 13 16 13H8C7.44772 13 7 12.5523 7 12C7 11.4477 7.44772 11 8 11H16Z"),
            )
        }.build()
        return _ic_control_indeterminate_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlIndeterminate24Preview() {
    Icon(
        imageVector = Icons.ic_control_indeterminate_24,
        contentDescription = null,
    )
}