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

private var _ic_scan_28: ImageVector? = null

val Icons.ic_scan_28: ImageVector
    get() {
        if (_ic_scan_28 != null) return _ic_scan_28!!
        _ic_scan_28 = ImageVector.Builder(
            name = "ic_scan_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M25.5195 13.3564C26.2099 13.3564 26.7695 13.9161 26.7695 14.6064C26.7694 15.2967 26.2098 15.8564 25.5195 15.8564H24.9502V18.8506C24.9501 22.2197 22.2188 24.9512 18.8496 24.9512H18.2441C17.5541 24.9509 16.9943 24.3913 16.9941 23.7012C16.9942 23.011 17.554 22.4514 18.2441 22.4512H18.8496C20.8381 22.4512 22.4501 20.839 22.4502 18.8506V15.8564H5.54883V18.8506C5.54891 20.8389 7.16116 22.4509 9.14941 22.4512H9.75488C10.4452 22.4512 11.0048 23.0109 11.0049 23.7012C11.0048 24.3914 10.4452 24.9512 9.75488 24.9512H9.14941C5.78045 24.9509 3.04891 22.2196 3.04883 18.8506V15.8564H2.47949C1.78943 15.8562 1.22962 15.2965 1.22949 14.6064C1.22954 13.9163 1.78938 13.3567 2.47949 13.3564H25.5195Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.75488 3.04883C10.4452 3.04883 11.0048 3.60851 11.0049 4.29883C11.0049 4.98918 10.4452 5.54883 9.75488 5.54883H9.14941C7.16114 5.54908 5.54887 7.16109 5.54883 9.14941V9.75586C5.54873 10.446 4.98895 11.0057 4.29883 11.0059C3.60875 11.0056 3.04893 10.446 3.04883 9.75586V9.14941C3.04887 5.78038 5.78043 3.04908 9.14941 3.04883H9.75488Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.8496 3.04883C22.2188 3.04883 24.9501 5.78022 24.9502 9.14941V9.75586C24.9501 10.4461 24.3905 11.0059 23.7002 11.0059C23.0103 11.0054 22.4503 10.4458 22.4502 9.75586V9.14941C22.4501 7.16093 20.8381 5.54883 18.8496 5.54883H18.2441C17.554 5.54857 16.9941 4.98903 16.9941 4.29883C16.9942 3.60867 17.554 3.04908 18.2441 3.04883H18.8496Z"),
            )
        }.build()
        return _ic_scan_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcScan28Preview() {
    Icon(
        imageVector = Icons.ic_scan_28,
        contentDescription = null,
    )
}