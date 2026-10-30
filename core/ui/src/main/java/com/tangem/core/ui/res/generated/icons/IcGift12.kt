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

private var _ic_gift_12: ImageVector? = null

val Icons.ic_gift_12: ImageVector
    get() {
        if (_ic_gift_12 != null) return _ic_gift_12!!
        _ic_gift_12 = ImageVector.Builder(
            name = "ic_gift_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.0498 1.46292C6.6434 0.845664 7.61431 0.845836 8.20801 1.46292C8.74468 2.02124 8.78627 2.89188 8.33496 3.50003H10C10.5522 3.50003 11 3.9479 11 4.50003V5.50003C10.9999 6.01743 10.6065 6.44279 10.1025 6.49417L10 6.50003V10C10 10.5522 9.55216 11 9 11H3C2.44784 11 2 10.5522 2 10V6.50003L1.89746 6.49417C1.39355 6.44279 1.00012 6.01743 1 5.50003V4.50003C1.00001 3.9479 1.44785 3.50003 2 3.50003H3.66504C3.2138 2.89189 3.25535 2.02121 3.79199 1.46292C4.3857 0.845784 5.35659 0.845601 5.9502 1.46292L6 1.51956L6.0498 1.46292ZM3 6.50003V10H9V6.50003H3ZM2 5.50003H10V4.50003H2V5.50003ZM7.4873 2.15628C7.28703 1.9482 6.97069 1.94803 6.77051 2.15628L6.71777 2.22269C6.66626 2.29859 6.61864 2.40525 6.58105 2.54691C6.53193 2.73239 6.50837 2.94608 6.50195 3.15726C6.49888 3.25905 6.49879 3.3578 6.50195 3.44827C6.80469 3.3861 7.15056 3.26162 7.40918 3.03421L7.4873 2.95902C7.69803 2.73954 7.69814 2.37568 7.4873 2.15628ZM5.22949 2.15628C5.02928 1.948 4.71297 1.94811 4.5127 2.15628C4.3018 2.37569 4.3019 2.73953 4.5127 2.95902L4.59082 3.03421C4.84884 3.26091 5.19375 3.38501 5.49609 3.4473C5.49923 3.35708 5.50111 3.25871 5.49805 3.15726C5.49163 2.94608 5.46807 2.73239 5.41895 2.54691C5.38139 2.40532 5.3337 2.2986 5.28223 2.22269L5.22949 2.15628Z"),
            )
        }.build()
        return _ic_gift_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGift12Preview() {
    Icon(
        imageVector = Icons.ic_gift_12,
        contentDescription = null,
    )
}