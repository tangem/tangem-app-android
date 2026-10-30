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

private var _ic_mask_12: ImageVector? = null

val Icons.ic_mask_12: ImageVector
    get() {
        if (_ic_mask_12 != null) return _ic_mask_12!!
        _ic_mask_12 = ImageVector.Builder(
            name = "ic_mask_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.50001 2.55646C9.50113 2.36978 9.41778 2.20307 9.29103 2.10236C9.16672 2.00363 9.01312 1.97621 8.87306 2.02033C6.99909 2.60959 5.00191 2.60959 3.12794 2.02033C2.98788 1.97621 2.83331 2.00362 2.709 2.10236C2.58225 2.20307 2.4989 2.36978 2.50001 2.55646V6.26349C2.50025 8.35453 4.09388 9.99982 6.00001 9.99982C7.90615 9.99982 9.49978 8.35453 9.50001 6.26349V2.55646ZM6.89455 6.97053C7.08895 6.77441 7.40546 6.77319 7.60158 6.9676C7.79767 7.16201 7.79989 7.47852 7.60548 7.67463C6.70988 8.57806 5.29015 8.57806 4.39455 7.67463C4.20014 7.47852 4.20236 7.16201 4.39845 6.9676C4.59456 6.77319 4.91108 6.77441 5.10548 6.97053C5.60977 7.47887 6.39026 7.47887 6.89455 6.97053ZM5.00001 4.17463C5.27616 4.17463 5.50001 4.39849 5.50001 4.67463C5.49979 4.95058 5.27602 5.17463 5.00001 5.17463H4.00001C3.72401 5.17462 3.50024 4.95058 3.50001 4.67463C3.50001 4.39849 3.72388 4.17463 4.00001 4.17463H5.00001ZM8.00001 4.17463C8.27615 4.17463 8.50001 4.39849 8.50001 4.67463C8.49979 4.95058 8.27602 5.17462 8.00001 5.17463H7.00001C6.72401 5.17463 6.50024 4.95058 6.50001 4.67463C6.50001 4.39849 6.72387 4.17463 7.00001 4.17463H8.00001ZM10.5 6.26349C10.4998 8.85186 8.51184 10.9998 6.00001 10.9998C3.48819 10.9998 1.50025 8.85186 1.50001 6.26349V2.55939C1.49797 2.07976 1.71041 1.61831 2.08693 1.31916C2.46646 1.01771 2.96527 0.920713 3.42873 1.06721L3.42775 1.06818C5.10616 1.59592 6.89286 1.59481 8.5713 1.06721C9.03475 0.92073 9.53358 1.01772 9.9131 1.31916C10.2896 1.61831 10.5021 2.07977 10.5 2.55939V6.26349Z"),
            )
        }.build()
        return _ic_mask_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMask12Preview() {
    Icon(
        imageVector = Icons.ic_mask_12,
        contentDescription = null,
    )
}