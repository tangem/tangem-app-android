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

private var _ic_arrow_up_20: ImageVector? = null

val Icons.ic_arrow_up_20: ImageVector
    get() {
        if (_ic_arrow_up_20 != null) return _ic_arrow_up_20!!
        _ic_arrow_up_20 = ImageVector.Builder(
            name = "ic_arrow_up_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.21926 9.51272C2.92715 9.21992 2.92701 8.74489 3.21926 8.45217L9.47023 2.2012C9.76296 1.90852 10.2388 1.90884 10.5318 2.2012L16.7827 8.45217C17.0756 8.745 17.0754 9.21981 16.7827 9.51272C16.4898 9.80518 16.0149 9.80547 15.7222 9.51272L10.7505 4.54202L10.7505 17.25C10.7504 17.6641 10.4147 18 10.0005 18C9.587 17.9993 9.25062 17.6637 9.25051 17.25L9.25051 4.54299L4.2798 9.51272C3.98687 9.80518 3.512 9.80547 3.21926 9.51272Z"),
            )
        }.build()
        return _ic_arrow_up_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_20,
        contentDescription = null,
    )
}