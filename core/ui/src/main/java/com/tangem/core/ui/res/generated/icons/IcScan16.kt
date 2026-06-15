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

private var _ic_scan_16: ImageVector? = null

val Icons.ic_scan_16: ImageVector
    get() {
        if (_ic_scan_16 != null) return _ic_scan_16!!
        _ic_scan_16 = ImageVector.Builder(
            name = "ic_scan_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.80371 7.64941C2.45873 7.64964 2.17871 7.92938 2.17871 8.27441C2.17914 8.61909 2.45899 8.89919 2.80371 8.89941H3V10.1885C3.00022 11.7418 4.25914 13.0017 5.8125 13.002H6.08691C6.43163 13.0016 6.7117 12.7217 6.71191 12.377C6.71191 12.032 6.43176 11.7523 6.08691 11.752H5.8125C4.94949 11.7517 4.25022 11.0515 4.25 10.1885V8.89941H11.7529V10.1885C11.7527 11.0515 11.0525 11.7518 10.1895 11.752H9.91602C9.57084 11.752 9.29102 12.0318 9.29102 12.377C9.29123 12.7219 9.57097 13.002 9.91602 13.002H10.1895C11.7429 13.0018 13.0027 11.7419 13.0029 10.1885V8.89941H13.1982C13.543 8.89926 13.8228 8.61913 13.8232 8.27441C13.8232 7.92933 13.5433 7.64957 13.1982 7.64941H2.80371Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.91602 2.99902C9.57084 2.99902 9.29102 3.27885 9.29102 3.62402C9.29123 3.96902 9.57097 4.24902 9.91602 4.24902H10.1895C11.0527 4.24919 11.7529 4.94925 11.7529 5.8125V6.08594C11.7532 6.43086 12.0329 6.71094 12.3779 6.71094C12.7227 6.71064 13.0026 6.43068 13.0029 6.08594V5.8125C13.0029 4.2589 11.743 2.99919 10.1895 2.99902H9.91602Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.8125 2.99902C4.259 2.99927 3 4.25894 3 5.8125V6.08594C3.0003 6.43086 3.28001 6.71094 3.625 6.71094C3.96974 6.71063 4.2497 6.43067 4.25 6.08594V5.8125C4.25 4.9493 4.94936 4.24927 5.8125 4.24902H6.08691C6.43163 4.24864 6.7117 3.96878 6.71191 3.62402C6.71191 3.27908 6.43177 2.99941 6.08691 2.99902H5.8125Z"),
            )
        }.build()
        return _ic_scan_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcScan16Preview() {
    Icon(
        imageVector = Icons.ic_scan_16,
        contentDescription = null,
    )
}