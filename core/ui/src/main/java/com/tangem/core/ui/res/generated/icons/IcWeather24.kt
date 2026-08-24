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

private var _ic_weather_24: ImageVector? = null

val Icons.ic_weather_24: ImageVector
    get() {
        if (_ic_weather_24 != null) return _ic_weather_24!!
        _ic_weather_24 = ImageVector.Builder(
            name = "ic_weather_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M13.8682 9.07251C16.2613 9.07257 18.2511 10.7987 18.6726 13.0651C20.0303 13.5703 20.9998 14.8723 21 16.4223C20.9999 18.3979 19.395 19.9999 17.418 20H8.77364C6.5817 20 4.80193 18.2232 4.80176 16.0337C4.80176 13.8423 6.5816 12.0665 8.77364 12.0665C8.96674 12.0665 9.15908 12.0809 9.34915 12.1075C10.0749 10.3272 11.8298 9.07269 13.8682 9.07251ZM13.8682 11.0718C12.4318 11.072 11.2343 12.1299 11.0258 13.4986C10.9776 13.8145 10.7812 14.0885 10.4972 14.2356C10.213 14.3826 9.87473 14.3846 9.58854 14.2414C9.36792 14.1311 9.09405 14.0658 8.77364 14.0657C7.68591 14.0657 6.80284 14.9473 6.80284 16.0337C6.80301 17.1182 7.68601 18.0008 8.77364 18.0008H17.418C18.2909 18.0007 18.9988 17.2927 18.9989 16.4223C18.9987 15.6015 18.3971 14.9476 17.6281 14.8496C17.1383 14.7873 16.7675 14.3772 16.7545 13.8842C16.7143 12.3286 15.4334 11.0718 13.8682 11.0718Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.0825 5.53674C12.3362 5.53681 13.4728 6.04576 14.2932 6.86534C14.6837 7.25562 14.6836 7.88851 14.2932 8.27887C13.9025 8.6692 13.2692 8.66912 12.8784 8.27887C12.4177 7.81867 11.7839 7.53605 11.0825 7.53598C9.6807 7.53606 8.5441 8.67161 8.54402 10.0721C8.54402 10.6241 8.09595 11.0716 7.54348 11.0718C6.9909 11.0718 6.54294 10.6242 6.54294 10.0721C6.54301 7.56746 8.57553 5.53682 11.0825 5.53674Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.70991 9.02468C5.26243 9.02475 5.71045 9.47227 5.71045 10.0243C5.71045 10.5763 5.26243 11.0238 4.70991 11.0239H4.00054C3.4481 11.0238 3 10.5763 3 10.0243C3 9.47232 3.4481 9.02484 4.00054 9.02468H4.70991Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.2869 4.36824C15.6742 3.97469 16.3077 3.96971 16.7018 4.35653C17.0954 4.74348 17.1016 5.37641 16.7145 5.77005L16.2181 6.27377C15.8309 6.66702 15.1973 6.67296 14.8033 6.28646C14.4093 5.89963 14.4046 5.26674 14.7916 4.87293L15.2869 4.36824Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.29812 4.31357C5.69217 3.9268 6.32565 3.93187 6.71295 4.32529L7.20932 4.82998C7.5963 5.22382 7.5907 5.85669 7.19661 6.24351C6.80246 6.63032 6.16903 6.62451 5.78178 6.23081L5.2864 5.7271C4.89926 5.33342 4.90434 4.70052 5.29812 4.31357Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.0024 2C11.555 2 12.0029 2.44852 12.0029 3.0006V3.70931C12.0026 4.2611 11.5548 4.70893 11.0024 4.70893C10.45 4.70893 10.0022 4.2611 10.0018 3.70931V3.0006C10.0018 2.44852 10.4498 2 11.0024 2Z"),
            )
        }.build()
        return _ic_weather_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWeather24Preview() {
    Icon(
        imageVector = Icons.ic_weather_24,
        contentDescription = null,
    )
}