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

private var _ic_chevron_right_16: ImageVector? = null

val Icons.ic_chevron_right_16: ImageVector
    get() {
        if (_ic_chevron_right_16 != null) return _ic_chevron_right_16!!
        _ic_chevron_right_16 = ImageVector.Builder(
            name = "ic_chevron_right_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.9971 3.18454C6.24115 2.94125 6.638 2.94094 6.88186 3.18454L11.2539 7.55759C11.4977 7.80143 11.4973 8.19727 11.2539 8.44137L6.88186 12.8144C6.63781 13.0585 6.24118 13.0584 5.9971 12.8144C5.75327 12.5703 5.7531 12.1737 5.9971 11.9297L9.92776 7.99997L5.9971 4.06833C5.75369 3.82423 5.75333 3.42841 5.9971 3.18454Z"),
            )
        }.build()
        return _ic_chevron_right_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronRight16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_right_16,
        contentDescription = null,
    )
}