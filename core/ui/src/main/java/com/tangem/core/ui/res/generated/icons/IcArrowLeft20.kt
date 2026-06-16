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

private var _ic_arrow_left_20: ImageVector? = null

val Icons.ic_arrow_left_20: ImageVector
    get() {
        if (_ic_arrow_left_20 != null) return _ic_arrow_left_20!!
        _ic_arrow_left_20 = ImageVector.Builder(
            name = "ic_arrow_left_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.51269 3.21852C9.21989 2.92641 8.74486 2.92628 8.45214 3.21852L2.20117 9.4695C1.90849 9.76222 1.90881 10.2381 2.20117 10.531L8.45214 16.782C8.74497 17.0748 9.21978 17.0747 9.51269 16.782C9.80515 16.4891 9.80544 16.0142 9.51269 15.7215L4.54199 10.7498H17.25C17.6641 10.7497 18 10.4139 18 9.99977C17.9992 9.58627 17.6636 9.24989 17.25 9.24977H4.54296L9.51269 4.27907C9.80515 3.98614 9.80544 3.51127 9.51269 3.21852Z"),
            )
        }.build()
        return _ic_arrow_left_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowLeft20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_left_20,
        contentDescription = null,
    )
}