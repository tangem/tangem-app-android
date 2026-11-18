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

private var _ic_chevron_double_vertical_collapse_28: ImageVector? = null

val Icons.ic_chevron_double_vertical_collapse_28: ImageVector
    get() {
        if (_ic_chevron_double_vertical_collapse_28 != null) return _ic_chevron_double_vertical_collapse_28!!
        _ic_chevron_double_vertical_collapse_28 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_collapse_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.3851 24.6509C19.8831 25.1287 20.6737 25.1127 21.1517 24.6147C21.6296 24.1168 21.6133 23.3262 21.1156 22.8481L14.8656 16.8481C14.3819 16.3839 13.6188 16.384 13.1351 16.8481L6.88508 22.8481C6.38708 23.3262 6.3709 24.1167 6.84895 24.6147C7.32705 25.1125 8.1176 25.1289 8.61555 24.6509L14.0003 19.481L19.3851 24.6509Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.1351 11.1509C13.6188 11.615 14.382 11.6151 14.8656 11.1509L21.1156 5.15093C21.6134 4.67293 21.6295 3.88234 21.1517 3.38433C20.6736 2.88635 19.8831 2.87019 19.3851 3.3482L14.0003 8.51812L8.61555 3.3482C8.11755 2.87016 7.32703 2.88637 6.84895 3.38433C6.3709 3.88235 6.38708 4.67285 6.88508 5.15093L13.1351 11.1509Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_collapse_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVerticalCollapse28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_collapse_28,
        contentDescription = null,
    )
}