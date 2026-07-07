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

private var _ic_sign_minus_28: ImageVector? = null

val Icons.ic_sign_minus_28: ImageVector
    get() {
        if (_ic_sign_minus_28 != null) return _ic_sign_minus_28!!
        _ic_sign_minus_28 = ImageVector.Builder(
            name = "ic_sign_minus_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M22 12.75C22.6904 12.75 23.25 13.3096 23.25 14C23.25 14.6904 22.6904 15.25 22 15.25H6C5.30964 15.25 4.75 14.6904 4.75 14C4.75 13.3096 5.30964 12.75 6 12.75H22Z"),
            )
        }.build()
        return _ic_sign_minus_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignMinus28Preview() {
    Icon(
        imageVector = Icons.ic_sign_minus_28,
        contentDescription = null,
    )
}