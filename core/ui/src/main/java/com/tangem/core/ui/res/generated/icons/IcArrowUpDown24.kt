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

private var _ic_arrow_up_down_24: ImageVector? = null

val Icons.ic_arrow_up_down_24: ImageVector
    get() {
        if (_ic_arrow_up_down_24 != null) return _ic_arrow_up_down_24!!
        _ic_arrow_up_down_24 = ImageVector.Builder(
            name = "ic_arrow_up_down_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.67193 3.75195C7.90506 3.75208 8.13031 3.83329 8.30865 3.98047C8.33674 4.00365 8.36333 4.02862 8.38872 4.05469L11.8585 7.52539C12.2489 7.91597 12.249 8.54899 11.8585 8.93945C11.4679 9.32983 10.8349 9.33078 10.4444 8.94043L8.67193 7.16602V19.25C8.67184 19.8022 8.22416 20.251 7.67193 20.251C7.12008 20.2507 6.67228 19.8028 6.67193 19.251V7.16602L4.89751 8.94043C4.50704 9.33038 3.87384 9.33052 3.48345 8.94043C3.09309 8.55006 3.0934 7.91693 3.48345 7.52637L6.96392 4.04492C7.15137 3.85747 7.40685 3.75205 7.67193 3.75195Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.3272 3.74902C16.8791 3.74921 17.3268 4.19715 17.3272 4.74902V16.8359L19.1016 15.0615C19.492 14.6711 20.1251 14.6713 20.5157 15.0615C20.9062 15.452 20.9062 16.0851 20.5157 16.4756L17.0352 19.957C16.8477 20.1445 16.5924 20.25 16.3272 20.25C16.0622 20.2498 15.8076 20.1445 15.6202 19.957L12.1407 16.4756C11.7503 16.085 11.7502 15.452 12.1407 15.0615C12.5312 14.6712 13.1643 14.6712 13.5547 15.0615L15.3272 16.835V4.75C15.3273 4.19779 15.775 3.74902 16.3272 3.74902Z"),
            )
        }.build()
        return _ic_arrow_up_down_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUpDown24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_down_24,
        contentDescription = null,
    )
}