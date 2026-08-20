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

private var _ic_earth_20: ImageVector? = null

val Icons.ic_earth_20: ImageVector
    get() {
        if (_ic_earth_20 != null) return _ic_earth_20!!
        _ic_earth_20 = ImageVector.Builder(
            name = "ic_earth_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2.00098C14.4267 2.00098 17.9989 5.5733 17.999 10C17.9989 14.4267 14.4267 17.999 10 17.999C5.57327 17.999 2.00111 14.4267 2.00098 10C2.00111 5.5733 5.57327 2.00098 10 2.00098ZM10 3.50098C8.89053 3.50098 7.84779 3.77772 6.93555 4.26465L8.25195 5.58105C8.81952 6.14862 9.13854 6.91911 9.13867 7.72168C9.13867 9.39391 7.78354 10.7499 6.11133 10.75H3.54492C3.91503 13.993 6.65539 16.499 10 16.499C11.5493 16.499 12.9682 15.9602 14.083 15.0605L13.0127 13.4551C12.9073 13.2969 12.7395 13.195 12.5547 13.1719L12.4746 13.166L12.3232 13.1611C11.57 13.1081 10.8958 12.6613 10.5547 11.9795L10.4473 11.7656C10.1453 11.1617 10.1455 10.4497 10.4473 9.8457L11.3594 8.02051L11.4316 7.8877C11.8157 7.23731 12.5183 6.83398 13.2803 6.83398H15.6797C14.5709 4.84354 12.4473 3.50098 10 3.50098ZM13.2803 8.33398C13.0347 8.33398 12.8109 8.47286 12.7021 8.69043L12.7012 8.69141L11.7891 10.5156C11.6983 10.6974 11.6982 10.913 11.7891 11.0947L11.8965 11.3086C12.0064 11.528 12.2299 11.666 12.4746 11.666C13.193 11.6661 13.8626 12.0259 14.2607 12.623L15.1543 13.9639C15.9972 12.8679 16.499 11.4945 16.499 10C16.499 9.42369 16.424 8.86544 16.2842 8.33398H13.2803ZM5.68555 5.13477C4.52012 6.16832 3.73155 7.61522 3.54492 9.25H6.11133C6.95512 9.24992 7.63867 8.56549 7.63867 7.72168C7.63854 7.31648 7.47735 6.92755 7.19141 6.6416L5.68555 5.13477Z"),
            )
        }.build()
        return _ic_earth_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEarth20Preview() {
    Icon(
        imageVector = Icons.ic_earth_20,
        contentDescription = null,
    )
}