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

private var _ic_dot_20_filled: ImageVector? = null

val Icons.ic_dot_20_filled: ImageVector
    get() {
        if (_ic_dot_20_filled != null) return _ic_dot_20_filled!!
        _ic_dot_20_filled = ImageVector.Builder(
            name = "ic_dot_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.66626 10C6.66626 11.8412 8.15883 13.3337 10 13.3337C11.8412 13.3337 13.3337 11.8412 13.3337 10C13.3337 8.15883 11.8412 6.66626 10 6.66626C8.15883 6.66626 6.66626 8.15883 6.66626 10Z"),
            )
        }.build()
        return _ic_dot_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDot20FilledPreview() {
    Icon(
        imageVector = Icons.ic_dot_20_filled,
        contentDescription = null,
    )
}