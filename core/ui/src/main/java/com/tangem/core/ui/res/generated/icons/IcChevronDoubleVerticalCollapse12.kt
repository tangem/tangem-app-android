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

private var _ic_chevron_double_vertical_collapse_12: ImageVector? = null

val Icons.ic_chevron_double_vertical_collapse_12: ImageVector
    get() {
        if (_ic_chevron_double_vertical_collapse_12 != null) return _ic_chevron_double_vertical_collapse_12!!
        _ic_chevron_double_vertical_collapse_12 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_collapse_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.1537 10.3578C8.35319 10.5483 8.66997 10.5412 8.86073 10.3422C9.05129 10.1426 9.04446 9.82589 8.8451 9.63517L6.3451 7.24845C6.15188 7.06408 5.84689 7.06402 5.6537 7.24845L3.1537 9.63517C2.95472 9.8259 2.9477 10.1427 3.13807 10.3422C3.32868 10.5414 3.64548 10.548 3.8451 10.3578L5.9994 8.30021L8.1537 10.3578Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.6537 4.74789C5.84678 4.93222 6.15186 4.93196 6.3451 4.74789L8.8451 2.36117C9.04446 2.17044 9.05129 1.85374 8.86073 1.65414C8.67 1.45481 8.35329 1.44796 8.1537 1.63851L5.9994 3.69613L3.8451 1.63851C3.6455 1.448 3.32878 1.4548 3.13807 1.65414C2.94793 1.85375 2.95453 2.17057 3.1537 2.36117L5.6537 4.74789Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_collapse_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVerticalCollapse12Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_collapse_12,
        contentDescription = null,
    )
}