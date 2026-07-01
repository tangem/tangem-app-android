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

private var _ic_chevron_expand_28: ImageVector? = null

val Icons.ic_chevron_expand_28: ImageVector
    get() {
        if (_ic_chevron_expand_28 != null) return _ic_chevron_expand_28!!
        _ic_chevron_expand_28 = ImageVector.Builder(
            name = "ic_chevron_expand_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4 12.75C4.69036 12.75 5.25 13.3096 5.25 14V22.5C5.25 22.6381 5.36193 22.75 5.5 22.75H14C14.6904 22.75 15.25 23.3096 15.25 24C15.25 24.6904 14.6904 25.25 14 25.25H5.5C3.98122 25.25 2.75 24.0188 2.75 22.5V14C2.75 13.3096 3.30964 12.75 4 12.75ZM22.5 2.75C24.0188 2.75 25.25 3.98122 25.25 5.5V14C25.25 14.6904 24.6904 15.25 24 15.25C23.3096 15.25 22.75 14.6904 22.75 14V5.5C22.75 5.36193 22.6381 5.25 22.5 5.25H14C13.3096 5.25 12.75 4.69036 12.75 4C12.75 3.30964 13.3096 2.75 14 2.75H22.5Z"),
            )
        }.build()
        return _ic_chevron_expand_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronExpand28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_expand_28,
        contentDescription = null,
    )
}