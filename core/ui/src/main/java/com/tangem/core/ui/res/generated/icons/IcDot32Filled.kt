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

private var _ic_dot_32_filled: ImageVector? = null

val Icons.ic_dot_32_filled: ImageVector
    get() {
        if (_ic_dot_32_filled != null) return _ic_dot_32_filled!!
        _ic_dot_32_filled = ImageVector.Builder(
            name = "ic_dot_32_filled",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.666 16C10.666 18.9459 13.0541 21.334 16 21.334C18.9459 21.334 21.334 18.9459 21.334 16C21.334 13.0541 18.9459 10.666 16 10.666C13.0541 10.666 10.666 13.0541 10.666 16Z"),
            )
        }.build()
        return _ic_dot_32_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDot32FilledPreview() {
    Icon(
        imageVector = Icons.ic_dot_32_filled,
        contentDescription = null,
    )
}