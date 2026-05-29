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

private var _ic_dot_16_filled: ImageVector? = null

val Icons.ic_dot_16_filled: ImageVector
    get() {
        if (_ic_dot_16_filled != null) return _ic_dot_16_filled!!
        _ic_dot_16_filled = ImageVector.Builder(
            name = "ic_dot_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.33301 8C5.33301 9.47294 6.52706 10.667 8 10.667C9.47294 10.667 10.667 9.47294 10.667 8C10.667 6.52706 9.47294 5.33301 8 5.33301C6.52706 5.33301 5.33301 6.52706 5.33301 8Z"),
            )
        }.build()
        return _ic_dot_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDot16FilledPreview() {
    Icon(
        imageVector = Icons.ic_dot_16_filled,
        contentDescription = null,
    )
}