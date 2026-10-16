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

private var _ic_box_28: ImageVector? = null

val Icons.ic_box_28: ImageVector
    get() {
        if (_ic_box_28 != null) return _ic_box_28!!
        _ic_box_28 = ImageVector.Builder(
            name = "ic_box_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.3525 2.93654C13.3088 2.39101 14.4711 2.35672 15.4531 2.834L15.6475 2.93654L22.8418 7.04494L23.0283 7.16017C23.4484 7.43828 23.7918 7.80601 24.042 8.22853C24.0474 8.23754 24.0543 8.24571 24.0596 8.2549C24.0633 8.26145 24.0657 8.26881 24.0693 8.2754C24.3455 8.75993 24.5 9.31398 24.5 9.89162V18.1084C24.5 19.2146 23.9408 20.2359 23.0283 20.8399L22.8418 20.9551L15.6475 25.0635C14.6912 25.609 13.5289 25.6433 12.5469 25.166L12.3525 25.0635L5.1582 20.9551L5.15723 20.9541C4.1362 20.37 3.5 19.2876 3.5 18.1074V9.89162C3.50001 8.71181 4.13584 7.62829 5.1582 7.04494L12.3525 2.93654ZM6 18.1074C6 18.381 6.14723 18.6404 6.39844 18.7842L12.75 22.4111V14.7246L6 10.8711V18.1074ZM15.25 14.7246V22.4102L21.6025 18.7842L21.6914 18.7246C21.8869 18.5759 22 18.348 22 18.1084V10.8711L15.25 14.7246ZM7.27051 8.71681L14 12.5596L16.1035 11.3574L9.37402 7.51564L7.27051 8.71681ZM14.4082 5.10744C14.156 4.96371 13.844 4.96455 13.5918 5.10841L13.5908 5.10744L11.8955 6.07521L18.625 9.91798L20.7285 8.71681L14.4082 5.10744Z"),
            )
        }.build()
        return _ic_box_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox28Preview() {
    Icon(
        imageVector = Icons.ic_box_28,
        contentDescription = null,
    )
}