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

private var _ic_arrow_refresh_24: ImageVector? = null

val Icons.ic_arrow_refresh_24: ImageVector
    get() {
        if (_ic_arrow_refresh_24 != null) return _ic_arrow_refresh_24!!
        _ic_arrow_refresh_24 = ImageVector.Builder(
            name = "ic_arrow_refresh_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20 11C20.5523 11 21 11.4477 21 12C21 16.971 16.971 21 12 21C9.16729 21 6.64691 19.6886 5 17.6455V18.666C5 19.2182 4.55214 19.6658 4 19.666C3.44783 19.6659 3 19.2182 3 18.666V15.333C3.0002 14.781 3.44795 14.3331 4 14.333H7.33301C7.88517 14.333 8.33281 14.7809 8.33301 15.333C8.33301 15.8853 7.88529 16.333 7.33301 16.333H6.51172C7.79335 17.9577 9.77446 19 12 19C15.8664 19 19 15.8664 19 12C19 11.4477 19.4477 11 20 11Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 3C14.8327 3.00005 17.3533 4.31128 19 6.35449V5.33301C19.0002 4.78089 19.4478 4.33301 20 4.33301C20.5522 4.33301 20.9998 4.78089 21 5.33301V8.66602C21 9.2183 20.5523 9.66602 20 9.66602H16.667C16.1147 9.66602 15.667 9.2183 15.667 8.66602C15.6674 8.11405 16.1149 7.66602 16.667 7.66602H17.4883C16.2066 6.04172 14.2252 5.00005 12 5C8.13373 5.00013 5 8.1337 5 12C5 12.5522 4.55221 12.9999 4 13C3.44783 12.9999 3 12.5522 3 12C3 7.02913 7.02916 3.00013 12 3Z"),
            )
        }.build()
        return _ic_arrow_refresh_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRefresh24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_refresh_24,
        contentDescription = null,
    )
}