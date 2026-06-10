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

private var _ic_arrow_up_28: ImageVector? = null

val Icons.ic_arrow_up_28: ImageVector
    get() {
        if (_ic_arrow_up_28 != null) return _ic_arrow_up_28!!
        _ic_arrow_up_28 = ImageVector.Builder(
            name = "ic_arrow_up_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7497 23.9167C12.7497 24.6069 13.3095 25.1665 13.9997 25.1667C14.69 25.1667 15.2497 24.607 15.2497 23.9167V7.68427L22.4499 14.8835C22.938 15.3716 23.7293 15.3716 24.2174 14.8835C24.7053 14.3953 24.7055 13.604 24.2174 13.1159L14.8835 3.7829C14.3953 3.29491 13.604 3.2948 13.1159 3.7829L3.78287 13.1159C3.29477 13.604 3.29487 14.3953 3.78287 14.8835C4.27102 15.3716 5.06229 15.3716 5.55045 14.8835L12.7497 7.68427V23.9167Z"),
            )
        }.build()
        return _ic_arrow_up_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_28,
        contentDescription = null,
    )
}