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

private var _ic_chevron_down_16: ImageVector? = null

val Icons.ic_chevron_down_16: ImageVector
    get() {
        if (_ic_chevron_down_16 != null) return _ic_chevron_down_16!!
        _ic_chevron_down_16 = ImageVector.Builder(
            name = "ic_chevron_down_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.928 5.99343C12.1721 5.74959 12.5688 5.74958 12.8128 5.99343C13.0565 6.23747 13.0566 6.6342 12.8128 6.8782L8.43975 11.2503C8.32261 11.3674 8.16302 11.4338 7.99736 11.4339C7.83191 11.4337 7.67303 11.3672 7.55596 11.2503L3.18291 6.8782C2.939 6.63411 2.9389 6.23746 3.18291 5.99343C3.42695 5.74959 3.82365 5.74958 4.06768 5.99343L7.99736 9.9241L11.928 5.99343Z"),
            )
        }.build()
        return _ic_chevron_down_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDown16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_down_16,
        contentDescription = null,
    )
}