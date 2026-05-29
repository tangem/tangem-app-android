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

private var _ic_info_24: ImageVector? = null

val Icons.ic_info_24: ImageVector
    get() {
        if (_ic_info_24 != null) return _ic_info_24!!
        _ic_info_24 = ImageVector.Builder(
            name = "ic_info_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 11C12.5523 11 13 11.4477 13 12V17C13 17.5523 12.5523 18 12 18C11.4477 18 11 17.5523 11 17V13C10.4477 13 10 12.5523 10 12C10 11.4477 10.4477 11 11 11H12Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.8662 7.10547C12.4453 7.16403 12.9004 7.65237 12.9004 8.25C12.9004 8.88506 12.3851 9.40039 11.75 9.40039C11.1545 9.40039 10.6642 8.94723 10.6055 8.36719L10.5996 8.25C10.5985 7.61163 11.1174 7.09961 11.749 7.09961L11.8662 7.10547Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5233 2 22 6.47672 22 12C22 17.5233 17.5233 22 12 22C6.47672 22 2 17.5233 2 12C2 6.47672 6.47672 2 12 2ZM12 4C7.58128 4 4 7.58128 4 12C4 16.4187 7.58128 20 12 20C16.4187 20 20 16.4187 20 12C20 7.58128 16.4187 4 12 4Z"),
            )
        }.build()
        return _ic_info_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcInfo24Preview() {
    Icon(
        imageVector = Icons.ic_info_24,
        contentDescription = null,
    )
}