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

private var _ic_chevron_up_24: ImageVector? = null

val Icons.ic_chevron_up_24: ImageVector
    get() {
        if (_ic_chevron_up_24 != null) return _ic_chevron_up_24!!
        _ic_chevron_up_24 = ImageVector.Builder(
            name = "ic_chevron_up_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.999 6.49902C12.2639 6.49925 12.5188 6.60473 12.7061 6.79199L19.7051 13.791C20.0952 14.1814 20.0949 14.8146 19.7051 15.2051C19.3146 15.5954 18.6815 15.5953 18.291 15.2051L11.999 8.91309L5.70706 15.2051C5.31661 15.5955 4.68352 15.5953 4.29299 15.2051C3.9026 14.8146 3.90256 14.1815 4.29299 13.791L11.292 6.79199C11.4794 6.60489 11.7342 6.49908 11.999 6.49902Z"),
            )
        }.build()
        return _ic_chevron_up_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronUp24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_up_24,
        contentDescription = null,
    )
}