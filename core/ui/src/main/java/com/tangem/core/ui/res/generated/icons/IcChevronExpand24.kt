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

private var _ic_chevron_expand_24: ImageVector? = null

val Icons.ic_chevron_expand_24: ImageVector
    get() {
        if (_ic_chevron_expand_24 != null) return _ic_chevron_expand_24!!
        _ic_chevron_expand_24 = ImageVector.Builder(
            name = "ic_chevron_expand_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4 11C4.55228 11 5 11.4477 5 12V19H12C12.5523 19 13 19.4477 13 20C13 20.5523 12.5523 21 12 21H5C3.89543 21 3 20.1046 3 19V12C3 11.4477 3.44772 11 4 11ZM19 3C20.1046 3 21 3.89543 21 5V12C21 12.5523 20.5523 13 20 13C19.4477 13 19 12.5523 19 12V5H12C11.4477 5 11 4.55228 11 4C11 3.44772 11.4477 3 12 3H19Z"),
            )
        }.build()
        return _ic_chevron_expand_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronExpand24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_expand_24,
        contentDescription = null,
    )
}