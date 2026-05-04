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

private var _ic_arrow_up_20: ImageVector? = null

val Icons.ic_arrow_up_20: ImageVector
    get() {
        if (_ic_arrow_up_20 != null) return _ic_arrow_up_20!!
        _ic_arrow_up_20 = ImageVector.Builder(
            name = "ic_arrow_up_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.25034 17.0833C9.25034 17.4975 9.58612 17.8333 10.0003 17.8333C10.4144 17.8331 10.7503 17.4974 10.7503 17.0833V5.14484L16.1361 10.5306C16.4289 10.8234 16.9037 10.8231 17.1966 10.5306C17.4895 10.2377 17.4895 9.76293 17.1966 9.47003L10.5306 2.80304C10.2378 2.5102 9.76297 2.51031 9.47006 2.80304L2.80307 9.47003C2.51034 9.76294 2.51023 10.2377 2.80307 10.5306C3.09592 10.8233 3.57076 10.8233 3.86362 10.5306L9.25034 5.14386V17.0833Z"),
            )
        }.build()
        return _ic_arrow_up_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_20,
        contentDescription = null,
    )
}