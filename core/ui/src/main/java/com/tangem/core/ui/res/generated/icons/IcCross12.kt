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

private var _ic_cross_12: ImageVector? = null

val Icons.ic_cross_12: ImageVector
    get() {
        if (_ic_cross_12 != null) return _ic_cross_12!!
        _ic_cross_12 = ImageVector.Builder(
            name = "ic_cross_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.15119 2.151C2.34646 1.9559 2.66301 1.95581 2.85822 2.151L6.00471 5.29651L9.15021 2.15198C9.34547 1.95676 9.66199 1.95676 9.85724 2.15198C10.0518 2.34729 10.0522 2.66399 9.85724 2.85901L6.71174 6.00452L9.85724 9.15002C10.0518 9.34535 10.0523 9.66301 9.85724 9.85803C9.66219 10.0526 9.3454 10.0515 9.15021 9.85705L6.00373 6.71057L2.85822 9.85705C2.66308 10.0521 2.34646 10.0519 2.15119 9.85705C1.95617 9.6618 1.95608 9.34522 2.15119 9.15002L5.2967 6.00354L2.15119 2.85803C1.95597 2.6628 1.95601 2.34627 2.15119 2.151Z"),
            )
        }.build()
        return _ic_cross_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCross12Preview() {
    Icon(
        imageVector = Icons.ic_cross_12,
        contentDescription = null,
    )
}