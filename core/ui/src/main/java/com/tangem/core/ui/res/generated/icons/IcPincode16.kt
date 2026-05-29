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

private var _ic_pincode_16: ImageVector? = null

val Icons.ic_pincode_16: ImageVector
    get() {
        if (_ic_pincode_16 != null) return _ic_pincode_16!!
        _ic_pincode_16 = ImageVector.Builder(
            name = "ic_pincode_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.6133 5.74316C10.9585 5.74316 11.2383 6.02299 11.2383 6.36816V9.63379C11.2382 9.97886 10.9584 10.2588 10.6133 10.2588C10.2685 10.2584 9.98841 9.97863 9.98828 9.63379V6.36816C9.98828 6.02322 10.2684 5.74354 10.6133 5.74316Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.56836 7.32617C4.88706 7.06676 5.34636 7.06671 5.66504 7.32617L5.73145 7.38574L5.81641 7.48438C5.99572 7.72703 6.03857 8.04943 5.9209 8.33398C5.7863 8.65912 5.46811 8.87107 5.11621 8.87109C4.76447 8.87093 4.44707 8.65898 4.3125 8.33398C4.17802 8.00883 4.253 7.63442 4.50195 7.38574L4.56836 7.32617Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.45312 7.32617C7.77182 7.06689 8.23118 7.06675 8.5498 7.32617L8.61621 7.38574L8.70117 7.48438C8.88038 7.72701 8.92331 8.04949 8.80566 8.33398C8.67113 8.65907 8.35278 8.87094 8.00098 8.87109C7.64928 8.87093 7.33186 8.65892 7.19727 8.33398C7.06278 8.00882 7.13774 7.63442 7.38672 7.38574L7.45312 7.32617Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00098 1.5C11.5913 1.50008 14.5018 4.41062 14.502 8.00098C14.5018 11.5913 11.5913 14.5019 8.00098 14.502C4.41066 14.5018 1.50015 11.5913 1.5 8.00098C1.50012 4.41064 4.41064 1.50012 8.00098 1.5ZM8.00098 2.75C5.101 2.75012 2.75012 5.101 2.75 8.00098C2.75015 10.9009 5.10101 13.2518 8.00098 13.252C10.901 13.2519 13.2518 10.901 13.252 8.00098C13.2518 5.10097 10.901 2.75008 8.00098 2.75Z"),
            )
        }.build()
        return _ic_pincode_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPincode16Preview() {
    Icon(
        imageVector = Icons.ic_pincode_16,
        contentDescription = null,
    )
}