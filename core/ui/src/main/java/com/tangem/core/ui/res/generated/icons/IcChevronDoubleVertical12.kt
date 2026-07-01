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

private var _ic_chevron_double_vertical_12: ImageVector? = null

val Icons.ic_chevron_double_vertical_12: ImageVector
    get() {
        if (_ic_chevron_double_vertical_12 != null) return _ic_chevron_double_vertical_12!!
        _ic_chevron_double_vertical_12 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.1537 7.2485C8.35319 7.05806 8.66997 7.06512 8.86073 7.26413C9.05129 7.46373 9.04446 7.78044 8.8451 7.97116L6.3451 10.3579C6.15188 10.5423 5.84689 10.5423 5.6537 10.3579L3.1537 7.97116C2.95472 7.78043 2.9477 7.46362 3.13807 7.26413C3.32868 7.06489 3.64548 7.05829 3.8451 7.2485L5.9994 9.30612L8.1537 7.2485Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.6537 1.63815C5.84678 1.45382 6.15186 1.45408 6.3451 1.63815L8.8451 4.02487C9.04446 4.2156 9.05129 4.5323 8.86073 4.7319C8.67 4.93123 8.35329 4.93808 8.1537 4.74753L5.9994 2.68991L3.8451 4.74753C3.6455 4.93804 3.32878 4.93124 3.13807 4.7319C2.94793 4.53229 2.95453 4.21547 3.1537 4.02487L5.6537 1.63815Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVertical12Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_12,
        contentDescription = null,
    )
}