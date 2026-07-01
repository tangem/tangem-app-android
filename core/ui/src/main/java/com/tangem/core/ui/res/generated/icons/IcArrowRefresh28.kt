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

private var _ic_arrow_refresh_28: ImageVector? = null

val Icons.ic_arrow_refresh_28: ImageVector
    get() {
        if (_ic_arrow_refresh_28 != null) return _ic_arrow_refresh_28!!
        _ic_arrow_refresh_28 = ImageVector.Builder(
            name = "ic_arrow_refresh_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.751 12.749C24.0606 12.749 23.501 13.3087 23.501 13.999C23.501 19.2457 19.248 23.4978 14.001 23.498C10.9116 23.498 8.16877 22.0188 6.43457 19.7275H7.72949C8.41975 19.7274 8.97949 19.1678 8.97949 18.4775C8.9793 17.7874 8.41963 17.2277 7.72949 17.2275H3.25C2.55993 17.2277 2.00019 17.7875 2 18.4775V22.9561C2 23.6463 2.55981 24.2059 3.25 24.2061C3.94026 24.2059 4.5 23.6463 4.5 22.9561V21.3115C6.69035 24.1575 10.1264 25.998 14.001 25.998C20.6286 25.9978 26.001 20.6265 26.001 13.999C26.001 13.3088 25.4412 12.7492 24.751 12.749Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C7.37245 2.0002 2.00018 7.37166 2 13.999C2.00013 14.6891 2.55994 15.2488 3.25 15.249C3.94022 15.249 4.49987 14.6892 4.5 13.999C4.50018 8.75249 8.75304 4.5002 14 4.5C17.0888 4.50004 19.8312 5.97872 21.5654 8.26953H20.2715C19.5815 8.26973 19.0218 8.82959 19.0215 9.51953C19.0215 10.2098 19.5813 10.7693 20.2715 10.7695H24.751C25.4412 10.7693 26.001 10.2098 26.001 9.51953V5.04102C26.0008 4.35091 25.4411 3.79122 24.751 3.79102C24.0607 3.79102 23.5011 4.35078 23.501 5.04102V6.6875C21.3106 3.84097 17.8749 2.00004 14 2Z"),
            )
        }.build()
        return _ic_arrow_refresh_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRefresh28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_refresh_28,
        contentDescription = null,
    )
}