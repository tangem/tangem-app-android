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

private var _ic_clock_32: ImageVector? = null

val Icons.ic_clock_32: ImageVector
    get() {
        if (_ic_clock_32 != null) return _ic_clock_32!!
        _ic_clock_32 = ImageVector.Builder(
            name = "ic_clock_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.75 8.77832C17.4404 8.77832 18 9.33796 18 10.0283V16.5986C17.9998 17.2888 17.4402 17.8486 16.75 17.8486H11.9717C11.2815 17.8486 10.7219 17.2888 10.7217 16.5986C10.7217 15.9083 11.2813 15.3486 11.9717 15.3486H15.5V10.0283C15.5 9.33801 16.0597 8.77839 16.75 8.77832Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.001 4C22.6288 4.00026 28.0027 9.37314 28.0029 16.001C28.0027 22.6288 22.6288 28.0027 16.001 28.0029C9.37314 28.0027 4.00026 22.6288 4 16.001C4.00026 9.37313 9.37313 4.00026 16.001 4ZM16.001 6.5C10.7538 6.50026 6.50026 10.7538 6.5 16.001C6.50026 21.2481 10.7538 25.5027 16.001 25.5029C21.2481 25.5027 25.5027 21.2481 25.5029 16.001C25.5027 10.7538 21.2481 6.50026 16.001 6.5Z"),
            )
        }.build()
        return _ic_clock_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClock32Preview() {
    Icon(
        imageVector = Icons.ic_clock_32,
        contentDescription = null,
    )
}