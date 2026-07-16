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

private var _ic_dot_28_filled: ImageVector? = null

val Icons.ic_dot_28_filled: ImageVector
    get() {
        if (_ic_dot_28_filled != null) return _ic_dot_28_filled!!
        _ic_dot_28_filled = ImageVector.Builder(
            name = "ic_dot_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.33252 13.9998C9.33252 16.5774 11.4221 18.667 13.9998 18.667C16.5774 18.667 18.667 16.5774 18.667 13.9998C18.667 11.4221 16.5774 9.33252 13.9998 9.33252C11.4221 9.33252 9.33252 11.4221 9.33252 13.9998Z"),
            )
        }.build()
        return _ic_dot_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDot28FilledPreview() {
    Icon(
        imageVector = Icons.ic_dot_28_filled,
        contentDescription = null,
    )
}