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

private var _ic_chevron_down_20: ImageVector? = null

val Icons.ic_chevron_down_20: ImageVector
    get() {
        if (_ic_chevron_down_20 != null) return _ic_chevron_down_20!!
        _ic_chevron_down_20 = ImageVector.Builder(
            name = "ic_chevron_down_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.7227 7.2392C16.0156 6.94632 16.4903 6.94631 16.7832 7.2392C17.0754 7.53214 17.0759 8.00707 16.7832 8.29974L10.5322 14.5517C10.3917 14.6922 10.1997 14.7713 10.001 14.7714C9.80255 14.7713 9.61121 14.6918 9.47072 14.5517L3.21974 8.29974C2.92686 8.00686 2.92688 7.53209 3.21974 7.2392C3.51264 6.94631 3.9874 6.94631 4.28029 7.2392L10.001 12.9599L15.7227 7.2392Z"),
            )
        }.build()
        return _ic_chevron_down_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDown20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_down_20,
        contentDescription = null,
    )
}