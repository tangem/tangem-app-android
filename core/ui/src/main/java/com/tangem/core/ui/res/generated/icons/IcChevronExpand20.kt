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

private var _ic_chevron_expand_20: ImageVector? = null

val Icons.ic_chevron_expand_20: ImageVector
    get() {
        if (_ic_chevron_expand_20 != null) return _ic_chevron_expand_20!!
        _ic_chevron_expand_20 = ImageVector.Builder(
            name = "ic_chevron_expand_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.75 9.25C4.16421 9.25 4.5 9.58579 4.5 10V15.5H10C10.4142 15.5 10.75 15.8358 10.75 16.25C10.75 16.6642 10.4142 17 10 17H4.5C3.67157 17 3 16.3284 3 15.5V10C3 9.58579 3.33579 9.25 3.75 9.25ZM15.5 3C16.3284 3 17 3.67157 17 4.5V10C17 10.4142 16.6642 10.75 16.25 10.75C15.8358 10.75 15.5 10.4142 15.5 10V4.5H10C9.58579 4.5 9.25 4.16421 9.25 3.75C9.25 3.33579 9.58579 3 10 3H15.5Z"),
            )
        }.build()
        return _ic_chevron_expand_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronExpand20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_expand_20,
        contentDescription = null,
    )
}