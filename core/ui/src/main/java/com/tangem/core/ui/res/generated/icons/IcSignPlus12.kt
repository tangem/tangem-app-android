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

private var _ic_sign_plus_12: ImageVector? = null

val Icons.ic_sign_plus_12: ImageVector
    get() {
        if (_ic_sign_plus_12 != null) return _ic_sign_plus_12!!
        _ic_sign_plus_12 = ImageVector.Builder(
            name = "ic_sign_plus_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.00488 2.00488C6.28098 2.00488 6.50482 2.2288 6.50488 2.50488V5.5H9.5C9.77614 5.5 10 5.72386 10 6C10 6.27614 9.77614 6.5 9.5 6.5H6.50488V9.50488C6.50488 9.78103 6.28103 10.0049 6.00488 10.0049C5.7288 10.0048 5.50488 9.78098 5.50488 9.50488V6.5H2.5C2.22386 6.5 2 6.27614 2 6C2 5.72386 2.22386 5.5 2.5 5.5H5.50488V2.50488C5.50495 2.22884 5.72884 2.00495 6.00488 2.00488Z"),
            )
        }.build()
        return _ic_sign_plus_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignPlus12Preview() {
    Icon(
        imageVector = Icons.ic_sign_plus_12,
        contentDescription = null,
    )
}