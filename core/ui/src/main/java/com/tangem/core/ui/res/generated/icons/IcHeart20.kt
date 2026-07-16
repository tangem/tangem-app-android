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

private var _ic_heart_20: ImageVector? = null

val Icons.ic_heart_20: ImageVector
    get() {
        if (_ic_heart_20 != null) return _ic_heart_20!!
        _ic_heart_20 = ImageVector.Builder(
            name = "ic_heart_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7783 3.5C15.6524 3.50026 17.5049 6.06504 17.5049 8.39062C17.5048 9.61013 17.0055 10.7261 16.3281 11.6816C15.6496 12.6388 14.7588 13.4824 13.8906 14.1748C13.0193 14.8697 12.1472 15.4304 11.4863 15.8174C11.1555 16.011 10.8741 16.1632 10.668 16.2686C10.5656 16.3208 10.478 16.3635 10.4102 16.3945C10.3773 16.4096 10.3423 16.4255 10.3096 16.4385C10.2941 16.4446 10.2699 16.454 10.2422 16.4629C10.2286 16.4673 10.2051 16.4738 10.1768 16.4805C10.1593 16.4845 10.0905 16.5009 10.002 16.501C9.91388 16.5009 9.84498 16.4846 9.82715 16.4805C9.79928 16.4739 9.77635 16.4673 9.7627 16.4629C9.73517 16.454 9.71083 16.4446 9.69531 16.4385C9.66267 16.4255 9.62759 16.4096 9.59473 16.3945C9.52688 16.3635 9.43839 16.3209 9.33594 16.2686C9.12976 16.1632 8.8483 16.011 8.51758 15.8174C7.85663 15.4304 6.98468 14.8698 6.11328 14.1748C5.24522 13.4825 4.35526 12.6387 3.67676 11.6816C2.99939 10.7261 2.50009 9.61015 2.5 8.39062C2.5 6.06492 4.35227 3.5 7.22656 3.5C8.44824 3.50012 9.36128 3.94253 10.002 4.45215C10.6427 3.94239 11.5562 3.5 12.7783 3.5ZM12.7783 5C11.6823 5 10.978 5.51661 10.5596 5.98145C10.4174 6.1392 10.2143 6.22942 10.002 6.22949C9.78956 6.22942 9.5865 6.13925 9.44434 5.98145C9.02594 5.51667 8.32236 5.00016 7.22656 5C5.34429 5 4 6.71969 4 8.39062C4.00009 9.19435 4.33061 10.0107 4.90039 10.8145C5.46908 11.6165 6.2442 12.3602 7.04883 13.002C7.85024 13.6411 8.65919 14.1617 9.27539 14.5225C9.57306 14.6967 9.82358 14.8313 10.002 14.9229C10.1804 14.8313 10.4307 14.6968 10.7285 14.5225C11.3448 14.1617 12.1545 13.6412 12.9561 13.002C13.7607 12.3602 14.5359 11.6166 15.1045 10.8145C15.6743 10.0107 16.0048 9.19433 16.0049 8.39062C16.0049 6.71982 14.6604 5.00027 12.7783 5Z"),
            )
        }.build()
        return _ic_heart_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart20Preview() {
    Icon(
        imageVector = Icons.ic_heart_20,
        contentDescription = null,
    )
}