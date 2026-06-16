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

private var _ic_arrow_down_24: ImageVector? = null

val Icons.ic_arrow_down_24: ImageVector
    get() {
        if (_ic_arrow_down_24 != null) return _ic_arrow_down_24!!
        _ic_arrow_down_24 = ImageVector.Builder(
            name = "ic_arrow_down_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11 4L11 18.0859L4.70703 11.793C4.31651 11.4024 3.6835 11.4024 3.29297 11.793C2.90245 12.1835 2.90245 12.8165 3.29297 13.207L11.293 21.207L11.3662 21.2734C11.5442 21.4193 11.7679 21.5 12 21.5C12.2652 21.5 12.5195 21.3946 12.707 21.207L20.707 13.207C21.0976 12.8165 21.0976 12.1835 20.707 11.793C20.3165 11.4024 19.6835 11.4024 19.293 11.793L13 18.0859L13 4C13 3.44772 12.5523 3 12 3C11.4477 3 11 3.44772 11 4Z"),
            )
        }.build()
        return _ic_arrow_down_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_24,
        contentDescription = null,
    )
}