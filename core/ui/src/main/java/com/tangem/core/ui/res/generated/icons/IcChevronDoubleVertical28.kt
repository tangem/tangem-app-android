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

private var _ic_chevron_double_vertical_28: ImageVector? = null

val Icons.ic_chevron_double_vertical_28: ImageVector
    get() {
        if (_ic_chevron_double_vertical_28 != null) return _ic_chevron_double_vertical_28!!
        _ic_chevron_double_vertical_28 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.3851 16.8481C19.8831 16.3702 20.6737 16.3863 21.1517 16.8842C21.6296 17.3822 21.6133 18.1728 21.1156 18.6508L14.8656 24.6508C14.3819 25.1151 13.6188 25.115 13.1351 24.6508L6.88508 18.6508C6.38708 18.1727 6.3709 17.3822 6.84895 16.8842C7.32705 16.3864 8.1176 16.3701 8.61555 16.8481L14.0003 22.018L19.3851 16.8481Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.1351 3.3481C13.6188 2.88406 14.382 2.88388 14.8656 3.3481L21.1156 9.3481C21.6134 9.82609 21.6295 10.6167 21.1517 11.1147C20.6736 11.6127 19.8831 11.6288 19.3851 11.1508L14.0003 5.98091L8.61555 11.1508C8.11755 11.6289 7.32703 11.6127 6.84895 11.1147C6.3709 10.6167 6.38708 9.82618 6.88508 9.3481L13.1351 3.3481Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVertical28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_28,
        contentDescription = null,
    )
}