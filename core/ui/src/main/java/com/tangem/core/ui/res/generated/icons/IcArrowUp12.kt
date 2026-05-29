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
                pathData = addPathNodes("M2.14681 5.35641C1.95207 5.16113 1.95178 4.84448 2.14681 4.64938L5.64779 1.14841C5.84291 0.953653 6.15963 0.953757 6.35482 1.14841L9.8558 4.64938C10.0507 4.84455 10.0506 5.16119 9.8558 5.35641C9.66059 5.55147 9.344 5.55142 9.14876 5.35641L6.5013 2.70895V10.5C6.5013 10.7759 6.27717 10.9996 6.0013 11C5.72542 10.9997 5.5013 10.7759 5.5013 10.5L5.5013 2.70895L2.85384 5.35641C2.65869 5.55149 2.34208 5.55131 2.14681 5.35641Z"),
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