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

private var _ic_scan_12: ImageVector? = null

val Icons.ic_scan_12: ImageVector
    get() {
        if (_ic_scan_12 != null) return _ic_scan_12!!
        _ic_scan_12 = ImageVector.Builder(
            name = "ic_scan_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.7466 5.74658C11.0226 5.74658 11.2464 5.97062 11.2466 6.24658C11.2464 6.52252 11.0226 6.74658 10.7466 6.74658H10.4966V7.99756C10.4961 9.37789 9.37702 10.4974 7.99661 10.4976H7.74661C7.47074 10.4975 7.24699 10.2734 7.24661 9.99756C7.24661 9.72144 7.47051 9.4976 7.74661 9.49756H7.99661C8.82475 9.49742 9.49614 8.82559 9.49661 7.99756V6.74658H2.49564V7.99756C2.49611 8.82555 3.16755 9.49736 3.99564 9.49756H4.24564C4.52178 9.49756 4.74564 9.72142 4.74564 9.99756C4.74526 10.2734 4.52155 10.4976 4.24564 10.4976H3.99564C2.61528 10.4974 1.49611 9.37785 1.49564 7.99756V6.74658H1.24564C0.969678 6.74654 0.745878 6.52249 0.745636 6.24658C0.745846 5.97064 0.969658 5.74662 1.24564 5.74658H10.7466Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.24564 1.49658C4.52178 1.49658 4.74564 1.72044 4.74564 1.99658C4.74539 2.27252 4.52163 2.49658 4.24564 2.49658H3.99564C3.16731 2.49678 2.49573 3.16827 2.49564 3.99658V4.24658C2.49539 4.52252 2.27163 4.74658 1.99564 4.74658C1.71971 4.7465 1.49588 4.52247 1.49564 4.24658V3.99658C1.49573 2.61597 2.61504 1.49678 3.99564 1.49658H4.24564Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99661 1.49658C9.37726 1.49672 10.4965 2.61593 10.4966 3.99658V4.24658C10.4964 4.5225 10.2726 4.74656 9.99661 4.74658C9.72065 4.74654 9.49685 4.52249 9.49661 4.24658V3.99658C9.49651 3.16823 8.82499 2.49672 7.99661 2.49658H7.74661C7.47065 2.49654 7.24685 2.27249 7.24661 1.99658C7.24661 1.72047 7.47051 1.49662 7.74661 1.49658H7.99661Z"),
            )
        }.build()
        return _ic_scan_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcScan12Preview() {
    Icon(
        imageVector = Icons.ic_scan_12,
        contentDescription = null,
    )
}