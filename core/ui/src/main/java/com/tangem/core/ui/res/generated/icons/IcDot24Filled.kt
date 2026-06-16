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

private var _ic_dot_24_filled: ImageVector? = null

val Icons.ic_dot_24_filled: ImageVector
    get() {
        if (_ic_dot_24_filled != null) return _ic_dot_24_filled!!
        _ic_dot_24_filled = ImageVector.Builder(
            name = "ic_dot_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99951 12C7.99951 14.2094 9.79059 16.0005 12 16.0005C14.2094 16.0005 16.0005 14.2094 16.0005 12C16.0005 9.79059 14.2094 7.99951 12 7.99951C9.79059 7.99951 7.99951 9.79059 7.99951 12Z"),
            )
        }.build()
        return _ic_dot_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDot24FilledPreview() {
    Icon(
        imageVector = Icons.ic_dot_24_filled,
        contentDescription = null,
    )
}