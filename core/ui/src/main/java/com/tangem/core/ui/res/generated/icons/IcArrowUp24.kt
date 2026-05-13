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

private var _ic_arrow_up_24: ImageVector? = null

val Icons.ic_arrow_up_24: ImageVector
    get() {
        if (_ic_arrow_up_24 != null) return _ic_arrow_up_24!!
        _ic_arrow_up_24 = ImageVector.Builder(
            name = "ic_arrow_up_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11 20.5C11 21.0523 11.4477 21.5 12 21.5C12.5523 21.5 13 21.0523 13 20.5V6.41406L19.293 12.707C19.6835 13.0976 20.3165 13.0976 20.707 12.707C21.0976 12.3165 21.0976 11.6835 20.707 11.293L12.707 3.29297C12.3165 2.90244 11.6835 2.90244 11.293 3.29297L3.29297 11.293C2.90245 11.6835 2.90245 12.3165 3.29297 12.707C3.68349 13.0976 4.31651 13.0976 4.70703 12.707L11 6.41406V20.5Z"),
            )
        }.build()
        return _ic_arrow_up_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_24,
        contentDescription = null,
    )
}