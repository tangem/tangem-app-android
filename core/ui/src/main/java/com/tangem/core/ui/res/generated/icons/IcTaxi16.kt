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

private var _ic_taxi_16: ImageVector? = null

val Icons.ic_taxi_16: ImageVector
    get() {
        if (_ic_taxi_16 != null) return _ic_taxi_16!!
        _ic_taxi_16 = ImageVector.Builder(
            name = "ic_taxi_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.05593 8.75C6.401 8.75013 6.68093 9.0299 6.68093 9.375C6.68093 9.7201 6.401 9.99987 6.05593 10H5.08425C4.73908 10 4.45925 9.72018 4.45925 9.375C4.45925 9.02982 4.73908 8.75 5.08425 8.75H6.05593Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.9163 8.75C11.2615 8.75 11.5413 9.02982 11.5413 9.375C11.5413 9.72018 11.2615 10 10.9163 10H9.9446C9.59943 10 9.3196 9.72018 9.3196 9.375C9.31961 9.02982 9.59943 8.75 9.9446 8.75H10.9163Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.80886 3C9.31428 3.00001 9.73778 3.34004 9.87429 3.80371L9.89675 3.89844L9.96902 4.25391C10.7876 4.30079 11.4698 4.89321 11.6555 5.69434L11.8675 6.60938L12.219 6.51953C12.5533 6.43357 12.8937 6.63551 12.9798 6.96973C13.0655 7.30391 12.8647 7.6445 12.5305 7.73047L12.4065 7.76172C12.7882 8.16025 12.9992 8.69349 12.9993 9.24609V10.375C12.9993 10.6627 12.9099 10.933 12.7571 11.1572V11.875C12.757 12.4794 12.2757 12.9999 11.6458 13H10.6741C10.044 13 9.56287 12.4795 9.56277 11.875V11.75H6.43777V11.875C6.43767 12.4794 5.95647 12.9999 5.32644 13H4.35476C3.72467 13 3.24353 12.4795 3.24343 11.875V11.1582C3.09015 10.9338 3.00027 10.663 3.00027 10.375V9.24707C3.00027 8.69451 3.21148 8.1603 3.59304 7.76172L3.47 7.73047C3.13596 7.64448 2.93418 7.30381 3.0198 6.96973C3.10572 6.63561 3.44642 6.43393 3.78054 6.51953L4.14773 6.61328L4.39968 5.64648C4.59875 4.87869 5.25468 4.31561 6.04031 4.25586L6.1282 3.87305L6.15261 3.78125C6.29617 3.32849 6.71435 3.00032 7.21023 3H8.80886ZM4.50027 8.62207C4.34217 8.78488 4.25027 9.00957 4.25027 9.24707V10.375C4.25027 10.4607 4.31378 10.4999 4.35476 10.5H11.6458C11.6868 10.4999 11.7493 10.4606 11.7493 10.375V9.24609C11.7492 9.00851 11.6575 8.78387 11.4993 8.62109L11.1389 8.25H4.8616L4.50027 8.62207ZM6.17898 5.5C5.92375 5.5 5.68289 5.67786 5.60964 5.95996L5.34011 7H10.6751L10.4378 5.97656C10.3704 5.68595 10.125 5.50006 9.86453 5.5H6.17898Z"),
            )
        }.build()
        return _ic_taxi_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTaxi16Preview() {
    Icon(
        imageVector = Icons.ic_taxi_16,
        contentDescription = null,
    )
}