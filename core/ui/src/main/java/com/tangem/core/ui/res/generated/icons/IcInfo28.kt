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

private var _ic_info_28: ImageVector? = null

val Icons.ic_info_28: ImageVector
    get() {
        if (_ic_info_28 != null) return _ic_info_28!!
        _ic_info_28 = ImageVector.Builder(
            name = "ic_info_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 12.75C14.6902 12.7502 15.25 13.3097 15.25 14V19.9717C15.25 20.6619 14.6902 21.2215 14 21.2217C13.3097 21.2216 12.75 20.662 12.75 19.9717V15.2461C12.0855 15.2169 11.5557 14.6717 11.5557 14C11.5557 13.3096 12.1153 12.75 12.8057 12.75H14Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.832 7.97949C14.6237 8.04632 15.2498 8.70702 15.25 9.52051C15.25 10.3756 14.5562 11.0692 13.7012 11.0693C12.8462 11.0691 12.1533 10.3755 12.1533 9.52051C12.1523 8.70625 12.7786 8.04689 13.5674 7.97949C13.611 7.97489 13.6554 7.97266 13.7002 7.97266C13.7446 7.97266 13.7888 7.97497 13.832 7.97949ZM13.6035 10.4678L13.7002 10.4727C13.6667 10.4727 13.6334 10.4694 13.6006 10.4668L13.6035 10.4678Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C20.6274 2.00019 25.9988 7.37257 25.999 14C25.9988 20.6275 20.6275 25.9988 14 25.999C7.37252 25.9989 2.00019 20.6275 2 14C2.00023 7.37256 7.37254 2.00016 14 2ZM14 4.5C8.75325 4.50016 4.50023 8.75327 4.5 14C4.50019 19.2468 8.75323 23.4989 14 23.499C19.2467 23.4988 23.4988 19.2467 23.499 14C23.4988 8.75329 19.2467 4.50019 14 4.5Z"),
            )
        }.build()
        return _ic_info_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcInfo28Preview() {
    Icon(
        imageVector = Icons.ic_info_28,
        contentDescription = null,
    )
}