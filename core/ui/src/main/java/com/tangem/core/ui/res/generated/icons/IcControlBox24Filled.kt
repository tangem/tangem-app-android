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

private var _ic_control_box_24_filled: ImageVector? = null

val Icons.ic_control_box_24_filled: ImageVector
    get() {
        if (_ic_control_box_24_filled != null) return _ic_control_box_24_filled!!
        _ic_control_box_24_filled = ImageVector.Builder(
            name = "ic_control_box_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17 3C19.2091 3 21 4.79086 21 7V17C21 19.2091 19.2091 21 17 21H7C4.79086 21 3 19.2091 3 17V7C3 4.79086 4.79086 3 7 3H17Z"),
            )
        }.build()
        return _ic_control_box_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlBox24FilledPreview() {
    Icon(
        imageVector = Icons.ic_control_box_24_filled,
        contentDescription = null,
    )
}