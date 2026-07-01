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

private var _ic_control_indeterminate_20: ImageVector? = null

val Icons.ic_control_indeterminate_20: ImageVector
    get() {
        if (_ic_control_indeterminate_20 != null) return _ic_control_indeterminate_20!!
        _ic_control_indeterminate_20 = ImageVector.Builder(
            name = "ic_control_indeterminate_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.25 9.25C13.6642 9.25 14 9.58579 14 10C14 10.4142 13.6642 10.75 13.25 10.75H6.75C6.33579 10.75 6 10.4142 6 10C6 9.58579 6.33579 9.25 6.75 9.25H13.25Z"),
            )
        }.build()
        return _ic_control_indeterminate_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlIndeterminate20Preview() {
    Icon(
        imageVector = Icons.ic_control_indeterminate_20,
        contentDescription = null,
    )
}