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

private var _ic_rose_12: ImageVector? = null

val Icons.ic_rose_12: ImageVector
    get() {
        if (_ic_rose_12 != null) return _ic_rose_12!!
        _ic_rose_12 = ImageVector.Builder(
            name = "ic_rose_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.42871 1.5C7.27879 1.50007 7.96788 2.10238 8.15918 2.8916C8.93941 3.09612 9.5 3.82479 9.5 4.66699C9.49989 5.3212 9.16038 5.90557 8.64355 6.23047V6.44531C8.64323 7.75828 7.73084 8.87409 6.5 9.11621V10C6.5 10.2761 6.27614 10.5 6 10.5C5.72405 10.4998 5.5 10.276 5.5 10V9.11621C4.26896 8.87435 3.35774 7.75851 3.35742 6.44531V5.90527C2.831 5.44381 2.5 4.75725 2.5 4C2.5 2.63671 3.57059 1.5 4.92871 1.5H6.42871ZM4.35742 6.44531C4.35778 7.41314 5.11017 8.16683 6 8.16699C6.87289 8.16672 7.61253 7.44128 7.64062 6.5H4.92871C4.73107 6.5 4.54047 6.4721 4.35742 6.42676V6.44531ZM4.92871 2.5C4.15684 2.5 3.5 3.1544 3.5 4C3.5 4.8456 4.15684 5.5 4.92871 5.5H7.71387C8.13079 5.5 8.49983 5.14443 8.5 4.66699C8.5 4.35117 8.33723 4.09084 8.10742 3.94922C7.86277 4.65094 7.21493 5.16693 6.42871 5.16699H5.17676C4.52877 5.16699 4.0332 4.62685 4.0332 4C4.0332 3.37315 4.52877 2.83301 5.17676 2.83301H6.0459C6.32193 2.83301 6.54572 3.05702 6.5459 3.33301C6.5459 3.60915 6.32204 3.83301 6.0459 3.83301H5.17676C5.11503 3.83301 5.0332 3.89085 5.0332 4C5.0332 4.10915 5.11503 4.16699 5.17676 4.16699H6.42871C6.84567 4.16691 7.21387 3.81055 7.21387 3.33301C7.2137 2.85563 6.84558 2.50008 6.42871 2.5H4.92871Z"),
            )
        }.build()
        return _ic_rose_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRose12Preview() {
    Icon(
        imageVector = Icons.ic_rose_12,
        contentDescription = null,
    )
}