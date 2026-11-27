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

private var _ic_lightning_12: ImageVector? = null

val Icons.ic_lightning_12: ImageVector
    get() {
        if (_ic_lightning_12 != null) return _ic_lightning_12!!
        _ic_lightning_12 = ImageVector.Builder(
            name = "ic_lightning_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.19497 1.89513C5.47843 1.51885 5.93154 1.43914 6.28482 1.54064C6.63963 1.64271 6.99264 1.96137 6.99282 2.4547V4.59142H9.00552C9.38463 4.59142 9.70898 4.79108 9.87759 5.08751C10.0486 5.38837 10.0526 5.78353 9.81118 6.10509L6.80532 10.1041C6.5218 10.4809 6.06798 10.5613 5.7145 10.4596C5.35972 10.3575 5.00669 10.0388 5.0065 9.54552V7.4088H2.9938C2.61489 7.40874 2.29146 7.20922 2.12271 6.91271C1.95138 6.61149 1.9472 6.21664 2.18911 5.89513L5.194 1.89611L5.19497 1.89513ZM5.99087 2.49962L3.05337 6.4088H5.5065C5.78254 6.4088 6.00634 6.63279 6.0065 6.9088V9.49962L8.94595 5.59142H6.49282C6.21694 5.59124 5.993 5.3673 5.99282 5.09142V2.49962H5.99087Z"),
            )
        }.build()
        return _ic_lightning_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLightning12Preview() {
    Icon(
        imageVector = Icons.ic_lightning_12,
        contentDescription = null,
    )
}