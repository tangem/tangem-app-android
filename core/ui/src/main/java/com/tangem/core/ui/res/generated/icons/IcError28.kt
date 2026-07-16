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

private var _ic_error_28: ImageVector? = null

val Icons.ic_error_28: ImageVector
    get() {
        if (_ic_error_28 != null) return _ic_error_28!!
        _ic_error_28 = ImageVector.Builder(
            name = "ic_error_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.1572 17.3887C14.932 17.4672 15.541 18.1209 15.541 18.9209C15.5407 19.7705 14.8515 20.4596 14.002 20.46C13.2054 20.4597 12.5487 19.854 12.4697 19.0781L12.4619 18.9209C12.4604 18.0663 13.1549 17.3813 14 17.3809L14.1572 17.3887Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.002 7.54395C14.692 7.54431 15.2519 8.10384 15.252 8.79395V14.5811C15.2515 15.2708 14.6917 15.8307 14.002 15.8311C13.3121 15.8308 12.7524 15.2709 12.752 14.5811V8.79395C12.752 8.10378 13.3118 7.54421 14.002 7.54395Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.79 2C18.755 2.00008 19.6812 2.38432 20.3633 3.06641L24.9346 7.6377C25.6187 8.3202 26.0039 9.24778 26.0039 10.2129V17.79C26.0038 18.6945 25.6665 19.5652 25.0625 20.2324L24.9375 20.3633L19.665 25.6377C19.4307 25.8721 19.1117 26.0038 18.7803 26.0039H10.2129C9.24798 26.0039 8.32177 25.6195 7.63965 24.9375L3.06641 20.3643C2.38434 19.6822 2.00007 18.756 2 17.791V10.2129C2 9.24784 2.38428 8.3218 3.06641 7.63965L7.63965 3.06641C8.3218 2.38429 9.24785 2 10.2129 2H17.79ZM10.2129 4.5C9.91164 4.5 9.62106 4.62018 9.40723 4.83398L4.83398 9.40723C4.62017 9.62107 4.5 9.91163 4.5 10.2129V17.791C4.50007 18.0922 4.62023 18.3829 4.83398 18.5967L9.40723 23.1699C9.62104 23.3836 9.91177 23.5039 10.2129 23.5039H18.2627L23.1699 18.5957L23.2461 18.5117C23.4121 18.3089 23.5038 18.0534 23.5039 17.79V10.2129C23.5039 9.91193 23.3838 9.62164 23.1699 9.4082L18.5957 4.83398C18.3819 4.62021 18.0912 4.50008 17.79 4.5H10.2129Z"),
            )
        }.build()
        return _ic_error_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcError28Preview() {
    Icon(
        imageVector = Icons.ic_error_28,
        contentDescription = null,
    )
}