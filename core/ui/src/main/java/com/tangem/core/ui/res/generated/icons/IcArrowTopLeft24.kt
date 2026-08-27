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

private var _ic_arrow_top_left_24: ImageVector? = null

val Icons.ic_arrow_top_left_24: ImageVector
    get() {
        if (_ic_arrow_top_left_24 != null) return _ic_arrow_top_left_24!!
        _ic_arrow_top_left_24 = ImageVector.Builder(
            name = "ic_arrow_top_left_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.49957 4.5C4.94793 4.50037 4.50011 4.9484 4.49957 5.5V15C4.49985 15.5518 4.94777 15.9996 5.49957 16C6.05169 16 6.4993 15.5521 6.49957 15V7.91406L17.7925 19.207C18.183 19.5973 18.8161 19.5971 19.2066 19.207C19.5968 18.8165 19.5969 18.1834 19.2066 17.793L7.91364 6.5H14.9996C15.5517 6.5 15.9993 6.05205 15.9996 5.5C15.999 4.94817 15.5515 4.5 14.9996 4.5H5.49957Z"),
            )
        }.build()
        return _ic_arrow_top_left_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopLeft24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_left_24,
        contentDescription = null,
    )
}