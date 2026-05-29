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

private var _ic_cross_28: ImageVector? = null

val Icons.ic_cross_28: ImageVector
    get() {
        if (_ic_cross_28 != null) return _ic_cross_28!!
        _ic_cross_28 = ImageVector.Builder(
            name = "ic_cross_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.8628 4.36688C22.3509 3.87882 23.1422 3.87887 23.6304 4.36688C24.1177 4.85502 24.1182 5.64653 23.6304 6.13446L15.7681 14.0007L23.6304 21.8669C24.1176 22.3552 24.1184 23.1467 23.6304 23.6345C23.1424 24.1219 22.3508 24.1208 21.8628 23.6335L13.9976 15.7663L6.13335 23.6335C5.64556 24.1213 4.85404 24.1216 4.36577 23.6345C3.8776 23.1465 3.87791 22.3551 4.36577 21.8669L12.2281 14.0007L4.36577 6.13446C3.87779 5.64631 3.87781 4.85497 4.36577 4.36688C4.85396 3.87889 5.64527 3.8788 6.13335 4.36688L13.9976 12.2341L21.8628 4.36688Z"),
            )
        }.build()
        return _ic_cross_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCross28Preview() {
    Icon(
        imageVector = Icons.ic_cross_28,
        contentDescription = null,
    )
}