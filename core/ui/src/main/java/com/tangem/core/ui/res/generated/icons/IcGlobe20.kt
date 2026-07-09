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

private var _ic_globe_20: ImageVector? = null

val Icons.ic_globe_20: ImageVector
    get() {
        if (_ic_globe_20 != null) return _ic_globe_20!!
        _ic_globe_20 = ImageVector.Builder(
            name = "ic_globe_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2.00195C14.4261 2.00222 17.998 5.57381 17.998 10C17.9978 14.426 14.426 17.9978 10 17.998C5.5738 17.998 2.00221 14.4261 2.00195 10C2.00195 5.57364 5.57364 2.00195 10 2.00195ZM3.5459 10.75C3.85242 13.4338 5.78224 15.6123 8.33496 16.2832C7.21091 14.6178 6.58578 12.6986 6.45801 10.75H3.5459ZM13.541 10.75C13.4132 12.6989 12.7875 14.6176 11.6631 16.2832C14.2165 15.6127 16.1476 13.4343 16.4541 10.75H13.541ZM7.96191 10.75C8.1043 12.6345 8.78249 14.4822 9.99902 16.0195C11.216 14.482 11.8957 12.6349 12.0381 10.75H7.96191ZM8.33594 3.71582C5.78248 4.38641 3.85224 6.56554 3.5459 9.25H6.45801C6.58571 7.30085 7.21138 5.38165 8.33594 3.71582ZM9.99902 3.97852C8.78184 5.5162 8.10423 7.36486 7.96191 9.25H12.0381C11.8958 7.36467 11.2164 5.5163 9.99902 3.97852ZM11.6631 3.71582C12.7876 5.38163 13.4133 7.30087 13.541 9.25H16.4541C16.1478 6.56544 14.2167 4.38633 11.6631 3.71582Z"),
            )
        }.build()
        return _ic_globe_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGlobe20Preview() {
    Icon(
        imageVector = Icons.ic_globe_20,
        contentDescription = null,
    )
}