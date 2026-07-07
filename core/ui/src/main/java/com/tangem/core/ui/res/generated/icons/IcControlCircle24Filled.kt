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

private var _ic_control_circle_24_filled: ImageVector? = null

val Icons.ic_control_circle_24_filled: ImageVector
    get() {
        if (_ic_control_circle_24_filled != null) return _ic_control_circle_24_filled!!
        _ic_control_circle_24_filled = ImageVector.Builder(
            name = "ic_control_circle_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2Z"),
            )
        }.build()
        return _ic_control_circle_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlCircle24FilledPreview() {
    Icon(
        imageVector = Icons.ic_control_circle_24_filled,
        contentDescription = null,
    )
}