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

private var _ic_pincode_20: ImageVector? = null

val Icons.ic_pincode_20: ImageVector
    get() {
        if (_ic_pincode_20 != null) return _ic_pincode_20!!
        _ic_pincode_20 = ImageVector.Builder(
            name = "ic_pincode_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.0039 7.37598C13.4181 7.37598 13.7539 7.71177 13.7539 8.12598V11.8789C13.7536 12.2928 13.4179 12.6289 13.0039 12.6289C12.59 12.6288 12.2542 12.2927 12.2539 11.8789V8.12598C12.2539 7.71186 12.5898 7.37613 13.0039 7.37598Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.95898 9.27246C6.34232 8.88986 6.95046 8.87284 7.35547 9.21875L7.36035 9.22168L7.41699 9.27246L7.46777 9.3291C7.47273 9.33522 7.47668 9.34242 7.48145 9.34863C7.72182 9.6405 7.78718 10.0421 7.64062 10.3965C7.48111 10.7819 7.10463 11.034 6.6875 11.0342C6.27036 11.0341 5.89395 10.7819 5.73438 10.3965C5.57495 10.011 5.66379 9.56729 5.95898 9.27246Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.27344 9.27246C9.65677 8.88977 10.2649 8.87281 10.6699 9.21875L10.6748 9.22168L10.7314 9.27246L10.7822 9.3291C10.7872 9.33524 10.7911 9.34239 10.7959 9.34863C11.0364 9.64053 11.1017 10.0421 10.9551 10.3965C10.7955 10.782 10.4192 11.0341 10.002 11.0342C9.58479 11.0341 9.20839 10.7819 9.04883 10.3965C8.8894 10.011 8.97824 9.56729 9.27344 9.27246Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.002 2.5C14.1451 2.50013 17.5047 5.85879 17.5049 10.002C17.5048 14.1451 14.1451 17.5048 10.002 17.5049C5.85879 17.5047 2.50013 14.1451 2.5 10.002C2.50014 5.8588 5.8588 2.50014 10.002 2.5ZM10.002 4C6.68723 4.00014 4.00014 6.68723 4 10.002C4.00013 13.3167 6.68722 16.0047 10.002 16.0049C13.3167 16.0048 16.0048 13.3167 16.0049 10.002C16.0047 6.68722 13.3167 4.00013 10.002 4Z"),
            )
        }.build()
        return _ic_pincode_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPincode20Preview() {
    Icon(
        imageVector = Icons.ic_pincode_20,
        contentDescription = null,
    )
}