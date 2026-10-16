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

private var _ic_clapper_board_16: ImageVector? = null

val Icons.ic_clapper_board_16: ImageVector
    get() {
        if (_ic_clapper_board_16 != null) return _ic_clapper_board_16!!
        _ic_clapper_board_16 = ImageVector.Builder(
            name = "ic_clapper_board_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.758 3.038C11.0099 2.97059 11.2766 2.99379 11.5109 3.10246L11.6086 3.15421L11.7013 3.21574C11.8795 3.34686 12.0146 3.52822 12.089 3.73624L12.1213 3.84171L12.384 4.89835C12.53 5.48496 12.1806 6.08411 11.5939 6.24113L9.174 6.88859H11.3908C12.0054 6.88883 12.5002 7.38854 12.5002 7.99992V11.4023C12.5001 12.2821 11.7885 12.9997 10.9064 12.9999H5.38982C4.50754 12.9999 3.79613 12.2822 3.79607 11.4023V7.58976L3.54802 6.59367C3.33774 5.7495 3.84058 4.88952 4.68279 4.66398L10.758 3.038ZM5.04607 11.4023C5.04613 11.5961 5.20214 11.7499 5.38982 11.7499H10.9064C11.0939 11.7497 11.2501 11.5959 11.2502 11.4023V8.13859H5.04607V11.4023ZM5.00603 5.87101C4.82591 5.91924 4.71456 6.10583 4.76091 6.29191L4.87322 6.74503L5.99626 6.44425V5.60539L5.00603 5.87101ZM7.24626 5.27042V6.10929L8.48747 5.77726V4.93839L7.24626 5.27042ZM9.73747 4.60343V5.4423L11.1379 5.06828L10.9426 4.28117L9.73747 4.60343Z"),
            )
        }.build()
        return _ic_clapper_board_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClapperBoard16Preview() {
    Icon(
        imageVector = Icons.ic_clapper_board_16,
        contentDescription = null,
    )
}