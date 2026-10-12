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

private var _ic_pie_chart_20: ImageVector? = null

val Icons.ic_pie_chart_20: ImageVector
    get() {
        if (_ic_pie_chart_20 != null) return _ic_pie_chart_20!!
        _ic_pie_chart_20 = ImageVector.Builder(
            name = "ic_pie_chart_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2C14.4186 2 18 5.58137 18 10C18 14.4186 14.4186 18 10 18C5.58137 18 2 14.4186 2 10C2 5.58137 5.58137 2 10 2ZM9.25 3.54395C6.01321 3.91578 3.5 6.6635 3.5 10C3.5 13.5902 6.4098 16.5 10 16.5C11.5248 16.5 12.9256 15.9736 14.0342 15.0947L9.70605 10.7666C9.57947 10.64 9.47788 10.4935 9.40234 10.335C9.37409 10.2847 9.35454 10.2318 9.33887 10.1787C9.28169 10.0152 9.25 9.84237 9.25 9.66602V3.54395ZM11.2207 10.1602L15.0947 14.0342C15.9736 12.9256 16.5 11.5248 16.5 10C16.5 9.08262 16.3086 8.21028 15.9658 7.41895L11.2207 10.1602ZM10.75 8.7002L15.2158 6.12012C14.1717 4.71872 12.5759 3.7537 10.75 3.54395V8.7002Z"),
            )
        }.build()
        return _ic_pie_chart_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPieChart20Preview() {
    Icon(
        imageVector = Icons.ic_pie_chart_20,
        contentDescription = null,
    )
}