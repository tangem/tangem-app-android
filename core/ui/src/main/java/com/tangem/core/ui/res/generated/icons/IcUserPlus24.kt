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

private var _ic_user_plus_24: ImageVector? = null

val Icons.ic_user_plus_24: ImageVector
    get() {
        if (_ic_user_plus_24 != null) return _ic_user_plus_24!!
        _ic_user_plus_24 = ImageVector.Builder(
            name = "ic_user_plus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.7739 14.0913C12.326 14.0916 12.7739 14.5392 12.7739 15.0913C12.7739 15.6434 12.326 16.0911 11.7739 16.0913H7.53467C6.13377 16.0913 4.99954 17.2258 4.99951 18.6235V19.5063C4.99944 20.0584 4.55154 20.5061 3.99951 20.5063C3.44727 20.5063 2.99959 20.0586 2.99951 19.5063V18.6235C2.99954 16.1197 5.03077 14.0913 7.53467 14.0913H11.7739Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.3491 13.2075C17.9014 13.2075 18.3491 13.6553 18.3491 14.2075V15.8569H20.0005C20.5528 15.8569 21.0005 16.3047 21.0005 16.8569C21.0005 17.4092 20.5528 17.8569 20.0005 17.8569H18.3491V19.5063C18.3489 20.0584 17.9013 20.5063 17.3491 20.5063C16.7972 20.5061 16.3493 20.0583 16.3491 19.5063V17.8569H14.6978C14.1456 17.8567 13.6978 17.4091 13.6978 16.8569C13.6978 16.3048 14.1456 15.8571 14.6978 15.8569H16.3491V14.2075C16.3491 13.6554 16.7971 13.2078 17.3491 13.2075Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0005 3.49365C14.5018 3.49391 16.5297 5.52161 16.5298 8.02295C16.5298 10.5244 14.5019 12.552 12.0005 12.5522C9.49888 12.5522 7.47022 10.5245 7.47021 8.02295C7.47034 5.52145 9.49895 3.49365 12.0005 3.49365ZM12.0005 5.49365C10.6032 5.49365 9.47034 6.62631 9.47021 8.02295C9.47022 9.41969 10.6032 10.5522 12.0005 10.5522C13.3976 10.552 14.5298 9.41953 14.5298 8.02295C14.5297 6.62647 13.3975 5.49391 12.0005 5.49365Z"),
            )
        }.build()
        return _ic_user_plus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus24Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_24,
        contentDescription = null,
    )
}