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

private var _ic_arrow_down_28: ImageVector? = null

val Icons.ic_arrow_down_28: ImageVector
    get() {
        if (_ic_arrow_down_28 != null) return _ic_arrow_down_28!!
        _ic_arrow_down_28 = ImageVector.Builder(
            name = "ic_arrow_down_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7496 4.66669L12.7496 20.8991L5.55042 13.6999C5.06226 13.2117 4.27099 13.2117 3.78284 13.6999C3.29485 14.1881 3.29474 14.9794 3.78284 15.4675L13.1158 24.8005C13.3502 25.0348 13.6682 25.1666 13.9996 25.1667C14.3311 25.1667 14.649 25.0348 14.8834 24.8005L24.2174 15.4675C24.7055 14.9794 24.7052 14.1881 24.2174 13.6999C23.7293 13.2117 22.938 13.2117 22.4498 13.6999L15.2496 20.9001L15.2496 4.66669C15.2496 3.97633 14.69 3.41669 13.9996 3.41669C13.3094 3.41686 12.7496 3.97644 12.7496 4.66669Z"),
            )
        }.build()
        return _ic_arrow_down_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_28,
        contentDescription = null,
    )
}