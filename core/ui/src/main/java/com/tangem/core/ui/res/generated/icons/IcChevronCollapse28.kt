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

private var _ic_chevron_collapse_28: ImageVector? = null

val Icons.ic_chevron_collapse_28: ImageVector
    get() {
        if (_ic_chevron_collapse_28 != null) return _ic_chevron_collapse_28!!
        _ic_chevron_collapse_28 = ImageVector.Builder(
            name = "ic_chevron_collapse_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9023 14.8711C12.5918 14.8723 13.1504 15.4326 13.1504 16.1221V24.5645C13.1503 25.2547 12.5907 25.8145 11.9004 25.8145C11.2101 25.8145 10.6505 25.2547 10.6504 24.5645V17.3701L3.45508 17.3564C2.76497 17.355 2.20589 16.7946 2.20703 16.1045C2.20826 15.4141 2.76962 14.8552 3.45996 14.8564L11.9023 14.8711ZM16.1221 2.20703C16.8121 2.20727 17.3719 2.76697 17.3721 3.45703V10.6504H24.5645C25.2544 10.6506 25.8141 11.2095 25.8145 11.8994C25.8145 12.5897 25.2546 13.1502 24.5645 13.1504H16.1221C15.4319 13.1503 14.8723 12.5905 14.8721 11.9004L14.8711 3.45703C14.8713 2.76686 15.4319 2.20708 16.1221 2.20703Z"),
            )
        }.build()
        return _ic_chevron_collapse_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronCollapse28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_collapse_28,
        contentDescription = null,
    )
}