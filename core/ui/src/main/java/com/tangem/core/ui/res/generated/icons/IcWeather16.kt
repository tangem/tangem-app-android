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

private var _ic_weather_16: ImageVector? = null

val Icons.ic_weather_16: ImageVector
    get() {
        if (_ic_weather_16 != null) return _ic_weather_16!!
        _ic_weather_16 = ImageVector.Builder(
            name = "ic_weather_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M9.16321 6.42149C10.6584 6.42149 11.8989 7.50023 12.1624 8.91618C13.0127 9.23084 13.6201 10.0456 13.6201 11.0154C13.6196 12.2493 12.6174 13.25 11.3824 13.25H5.97776C4.60836 13.25 3.49656 12.1397 3.49629 10.7719C3.49629 9.40273 4.60819 8.29279 5.97776 8.29275C6.09981 8.29276 6.22135 8.30205 6.34145 8.31905C6.79496 7.20652 7.88929 6.42152 9.16321 6.42149ZM9.16321 7.66835C8.2644 7.66839 7.51518 8.33059 7.38474 9.18699C7.35452 9.38389 7.2313 9.55517 7.0542 9.64677C6.87723 9.73805 6.66686 9.73958 6.48868 9.65066C6.35042 9.58148 6.17851 9.5406 5.97776 9.54059C5.29693 9.54062 4.74434 10.0919 4.74434 10.7719C4.74461 11.4505 5.2971 12.0031 5.97776 12.0031H11.3824C11.9287 12.0031 12.3716 11.5601 12.3721 11.0154C12.3721 10.5015 11.9955 10.092 11.514 10.0306C11.2086 9.99169 10.9772 9.735 10.969 9.42759C10.9437 8.45443 10.1425 7.66835 9.16321 7.66835Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.42082 4.21123C8.20427 4.21123 8.91479 4.52903 9.42745 5.04117C9.67097 5.28453 9.67079 5.67927 9.42745 5.92274C9.18374 6.16576 8.7886 6.166 8.54504 5.92274C8.25673 5.63474 7.85979 5.45809 7.42082 5.45809C6.54391 5.45843 5.83262 6.1688 5.83248 7.04492C5.83222 7.389 5.55293 7.66835 5.20846 7.66835C4.86415 7.66815 4.58469 7.38888 4.58443 7.04492C4.58457 5.48018 5.85463 4.21157 7.42082 4.21123Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.43778 6.39129C3.78242 6.39129 4.06181 6.67041 4.06181 7.01472C4.06181 7.35903 3.78242 7.63815 3.43778 7.63815H2.99414C2.64953 7.63812 2.37012 7.35901 2.37012 7.01472C2.37012 6.67043 2.64953 6.39132 2.99414 6.39129H3.43778Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0515 3.47967C10.293 3.23442 10.6881 3.23088 10.9339 3.47188C11.1797 3.71319 11.1832 4.10883 10.9417 4.35442L10.6316 4.67004C10.3901 4.91532 9.99398 4.91897 9.74824 4.67783C9.5027 4.43676 9.49966 4.0419 9.74044 3.79626L10.0515 3.47967Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.80635 3.44461C4.0522 3.20365 4.44831 3.20694 4.68973 3.4524L4.9998 3.76898C5.241 4.0145 5.23743 4.4093 4.992 4.65055C4.74622 4.89163 4.35109 4.88809 4.10959 4.64276L3.79855 4.32715C3.55705 4.08154 3.56053 3.6859 3.80635 3.44461Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.37109 2C7.71573 2 7.99512 2.27912 7.99512 2.62343V3.06665C7.99512 3.41096 7.71573 3.69008 7.37109 3.69008C7.02659 3.68992 6.74707 3.41086 6.74707 3.06665V2.62343C6.74707 2.27922 7.02659 2.00016 7.37109 2Z"),
            )
        }.build()
        return _ic_weather_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWeather16Preview() {
    Icon(
        imageVector = Icons.ic_weather_16,
        contentDescription = null,
    )
}