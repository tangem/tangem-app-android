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

private var _ic_football_16: ImageVector? = null

val Icons.ic_football_16: ImageVector
    get() {
        if (_ic_football_16 != null) return _ic_football_16!!
        _ic_football_16 = ImageVector.Builder(
            name = "ic_football_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.75773 3.75783C6.10088 1.41471 9.89896 1.41471 12.2421 3.75783L12.455 3.98146C14.5827 6.33678 14.5119 9.97245 12.2421 12.2422C9.8991 14.5851 6.10089 14.5848 3.75773 12.2422C1.41457 9.89908 1.41459 6.10095 3.75773 3.75783ZM6.9589 10.5L6.63858 10.9414L7.26651 12.6914C7.75209 12.7669 8.24676 12.7669 8.73233 12.6914L9.36026 10.9414L9.03995 10.5H6.9589ZM4.71573 11.4287C5.03492 11.7346 5.38539 11.988 5.75773 12.1875L5.49405 11.4522L4.71573 11.4287ZM10.5038 11.4522L10.2392 12.1875C10.6121 11.9879 10.9635 11.735 11.2831 11.4287L10.5038 11.4522ZM3.31144 8.75881C3.38865 9.23802 3.53916 9.70753 3.76358 10.1494L5.62687 10.2061L5.94718 9.76369L5.30558 7.78908L4.78409 7.61916L3.31144 8.75881ZM10.6923 7.7881L10.0507 9.76369L10.372 10.2061L12.2333 10.1494C12.4578 9.7076 12.61 9.23896 12.6874 8.75978L11.2128 7.61916L10.6923 7.7881ZM6.53605 7.52931L7.09464 9.25002H8.90421L9.4628 7.52931L7.99991 6.46681L6.53605 7.52931ZM12.0517 6.68752L12.6757 7.16994C12.6003 6.74247 12.4664 6.32358 12.2733 5.9258L12.0517 6.68752ZM3.72355 5.92971C3.53182 6.32567 3.39842 6.74265 3.32315 7.16799L3.94523 6.68752L3.72355 5.92971ZM5.82608 3.77541C5.40223 3.99367 5.00412 4.27956 4.64835 4.63381L5.17276 6.43166L5.69034 6.59963L7.37394 5.37599V4.82912L5.82608 3.77541ZM8.62394 4.83107V5.37502L10.3085 6.59963L10.8251 6.43166L11.3495 4.63283L11.1816 4.47267C10.8708 4.19189 10.5311 3.95946 10.1737 3.77541L8.62394 4.83107ZM8.65616 3.29592C8.22052 3.23549 7.77833 3.23536 7.34269 3.29592L7.99991 3.74318L8.65616 3.29592Z"),
            )
        }.build()
        return _ic_football_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFootball16Preview() {
    Icon(
        imageVector = Icons.ic_football_16,
        contentDescription = null,
    )
}