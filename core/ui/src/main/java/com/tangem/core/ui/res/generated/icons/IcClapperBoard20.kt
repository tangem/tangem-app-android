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

private var _ic_clapper_board_20: ImageVector? = null

val Icons.ic_clapper_board_20: ImageVector
    get() {
        if (_ic_clapper_board_20 != null) return _ic_clapper_board_20!!
        _ic_clapper_board_20 = ImageVector.Builder(
            name = "ic_clapper_board_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.2864 3.01759C14.5701 2.97311 14.8624 3.01164 15.1253 3.12892L15.2542 3.19435L15.3763 3.27247C15.6514 3.46739 15.8541 3.75409 15.9407 4.0879L16.3314 5.59767C16.5356 6.38735 16.0405 7.15479 15.2806 7.3506L10.6048 8.55568H15.0325C15.8188 8.556 16.5011 9.17858 16.5013 10V14.8613C16.5013 16.0665 15.4971 16.9997 14.3138 17H6.12823C4.94463 17 3.94074 16.0667 3.94073 14.8613V9.40626L3.56768 7.9629C3.26859 6.80563 3.99152 5.66391 5.13018 5.37013L14.1458 3.04689L14.2864 3.01759ZM5.44073 14.8613C5.44074 15.1901 5.72408 15.5 6.12823 15.5H14.3138C14.7176 15.4997 15.0013 15.19 15.0013 14.8613V10.0557H5.44073V14.8613ZM5.50518 6.82228C5.12773 6.91969 4.93908 7.27299 5.01983 7.58693L5.22784 8.39161L7.20538 7.88185V6.3838L5.50518 6.82228ZM8.70538 5.99708V7.49513L10.9017 6.9297V5.43165L8.70538 5.99708ZM12.4017 5.04493V6.54298L14.8626 5.90919L14.4983 4.5049L12.4017 5.04493Z"),
            )
        }.build()
        return _ic_clapper_board_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClapperBoard20Preview() {
    Icon(
        imageVector = Icons.ic_clapper_board_20,
        contentDescription = null,
    )
}