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

private var _ic_weather_20: ImageVector? = null

val Icons.ic_weather_20: ImageVector
    get() {
        if (_ic_weather_20 != null) return _ic_weather_20!!
        _ic_weather_20 = ImageVector.Builder(
            name = "ic_weather_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M11.5546 7.88007C13.5194 7.88049 15.1511 9.3096 15.4756 11.178C16.5958 11.5743 17.4 12.6384 17.4001 13.9082C17.4001 15.5048 16.1034 16.7997 14.5056 16.8H7.31765C5.54095 16.8 4.09852 15.3595 4.09852 13.5848C4.09858 11.8087 5.54099 10.3696 7.31765 10.3696C7.49625 10.3697 7.67427 10.3834 7.84897 10.4115C8.42475 8.93007 9.87149 7.88007 11.5546 7.88007ZM11.5546 9.37614C10.3187 9.37614 9.2881 10.2862 9.10855 11.4644C9.07254 11.7009 8.92524 11.9066 8.71274 12.0167C8.50017 12.1266 8.24731 12.1275 8.03323 12.0206C7.83685 11.9223 7.59548 11.8658 7.31765 11.8657C6.36737 11.8657 5.59603 12.6356 5.59597 13.5848C5.59597 14.5326 6.36733 15.3039 7.31765 15.3039H14.5056C15.2772 15.3036 15.9026 14.6778 15.9026 13.9082C15.9026 13.1836 15.3711 12.6053 14.6908 12.5183C14.3243 12.4716 14.0463 12.1646 14.0367 11.7956C14.0021 10.4576 12.901 9.37661 11.5546 9.37614Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.23626 4.94052C10.2556 4.94052 11.1799 5.35433 11.8471 6.02069C12.1394 6.31279 12.1394 6.78633 11.8471 7.07846C11.5546 7.37025 11.0806 7.37047 10.7883 7.07846C10.3903 6.68103 9.84231 6.43659 9.23626 6.43659C8.02521 6.43706 7.04297 7.41811 7.04273 8.6281C7.0426 9.04112 6.70743 9.37614 6.294 9.37614C5.88077 9.37591 5.54541 9.04098 5.54528 8.6281C5.54552 6.59186 7.19819 4.94099 9.23626 4.94052Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.93864 7.84013C4.35201 7.8402 4.68723 8.17519 4.68736 8.58817C4.68736 9.00126 4.35209 9.33614 3.93864 9.3362H3.34882C2.93543 9.33607 2.6001 9.00121 2.6001 8.58817C2.60023 8.17523 2.93551 7.84026 3.34882 7.84013H3.93864Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7927 3.94411C13.0825 3.64958 13.5565 3.64495 13.8515 3.93437C14.1461 4.22376 14.1505 4.69746 13.8612 4.99214L13.4488 5.41194C13.1591 5.7064 12.685 5.70997 12.3901 5.4207C12.0951 5.13119 12.0906 4.65764 12.3803 4.36294L12.7927 3.94411Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.48653 3.89931C4.78152 3.60997 5.25555 3.61441 5.54528 3.90905L5.95766 4.32787C6.24713 4.62257 6.24271 5.0962 5.94791 5.38564C5.65307 5.67491 5.17896 5.67112 4.88917 5.37687L4.47679 4.95708C4.18722 4.66241 4.19175 4.18879 4.48653 3.89931Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.17094 2C9.58427 2.00016 9.9196 2.33506 9.91967 2.74804V3.33731C9.9196 3.75028 9.58427 4.08518 9.17094 4.08534C8.75747 4.08534 8.42228 3.75038 8.42222 3.33731V2.74804C8.42228 2.33496 8.75747 2 9.17094 2Z"),
            )
        }.build()
        return _ic_weather_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWeather20Preview() {
    Icon(
        imageVector = Icons.ic_weather_20,
        contentDescription = null,
    )
}