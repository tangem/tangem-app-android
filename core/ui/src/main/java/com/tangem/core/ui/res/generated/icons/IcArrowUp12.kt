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

private var _ic_arrow_up_12: ImageVector? = null

val Icons.ic_arrow_up_12: ImageVector
    get() {
        if (_ic_arrow_up_12 != null) return _ic_arrow_up_12!!
        _ic_arrow_up_12 = ImageVector.Builder(
            name = "ic_arrow_up_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.5 10.25C5.5 10.5261 5.72386 10.75 6 10.75C6.27614 10.75 6.5 10.5261 6.5 10.25V3.20703L9.64648 6.35352C9.84175 6.54878 10.1583 6.54878 10.3535 6.35352C10.5488 6.15825 10.5488 5.84175 10.3535 5.64648L6.35352 1.64648C6.15825 1.45122 5.84175 1.45122 5.64648 1.64648L1.64648 5.64648C1.45122 5.84175 1.45122 6.15825 1.64648 6.35352C1.84175 6.54878 2.15825 6.54878 2.35352 6.35352L5.5 3.20703V10.25Z"),
            )
        }.build()
        return _ic_arrow_up_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_12,
        contentDescription = null,
    )
}