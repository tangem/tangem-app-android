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

private var _ic_arrow_left_28: ImageVector? = null

val Icons.ic_arrow_left_28: ImageVector
    get() {
        if (_ic_arrow_left_28 != null) return _ic_arrow_left_28!!
        _ic_arrow_left_28 = ImageVector.Builder(
            name = "ic_arrow_left_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.249 4.35978C11.7405 3.8754 12.532 3.88125 13.0166 4.37248C13.5009 4.86406 13.4952 5.65549 13.0039 6.14006L6.29785 12.7514H24.75C25.4401 12.7515 25.9998 13.3113 26 14.0014C25.9997 14.6914 25.4401 15.2513 24.75 15.2514H6.29883L13.0039 21.8617C13.4952 22.3464 13.501 23.1387 13.0166 23.6303C12.532 24.1214 11.7406 24.1271 11.249 23.643L2.37207 14.891C2.13429 14.6563 2.00016 14.3355 2 14.0014C2.00012 13.667 2.13402 13.3455 2.37207 13.1108L11.249 4.35978Z"),
            )
        }.build()
        return _ic_arrow_left_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowLeft28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_left_28,
        contentDescription = null,
    )
}