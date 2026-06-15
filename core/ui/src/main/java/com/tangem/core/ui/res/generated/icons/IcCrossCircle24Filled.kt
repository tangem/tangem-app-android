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

private var _ic_cross_circle_24_filled: ImageVector? = null

val Icons.ic_cross_circle_24_filled: ImageVector
    get() {
        if (_ic_cross_circle_24_filled != null) return _ic_cross_circle_24_filled!!
        _ic_cross_circle_24_filled = ImageVector.Builder(
            name = "ic_cross_circle_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2ZM16.207 7.79297C15.8165 7.40292 15.1833 7.4026 14.793 7.79297L12 10.5859L9.20703 7.79297C8.81649 7.40266 8.18342 7.40252 7.79297 7.79297C7.40261 8.18343 7.40269 8.81652 7.79297 9.20703L10.5859 12L7.79297 14.793C7.40261 15.1834 7.40269 15.8175 7.79297 16.208C8.18343 16.5978 8.81669 16.598 9.20703 16.208L12 13.4141L14.793 16.207C15.1835 16.5971 15.8166 16.5973 16.207 16.207C16.5974 15.8166 16.5972 15.1835 16.207 14.793L13.4141 12L16.207 9.20703C16.5974 8.81664 16.5972 8.18352 16.207 7.79297Z"),
            )
        }.build()
        return _ic_cross_circle_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCrossCircle24FilledPreview() {
    Icon(
        imageVector = Icons.ic_cross_circle_24_filled,
        contentDescription = null,
    )
}