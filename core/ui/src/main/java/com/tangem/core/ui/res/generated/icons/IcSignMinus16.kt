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

private var _ic_sign_minus_16: ImageVector? = null

val Icons.ic_sign_minus_16: ImageVector
    get() {
        if (_ic_sign_minus_16 != null) return _ic_sign_minus_16!!
        _ic_sign_minus_16 = ImageVector.Builder(
            name = "ic_sign_minus_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5 7.375C12.8452 7.375 13.125 7.65482 13.125 8C13.125 8.34518 12.8452 8.625 12.5 8.625H3.5C3.15482 8.625 2.875 8.34518 2.875 8C2.875 7.65482 3.15482 7.375 3.5 7.375H12.5Z"),
            )
        }.build()
        return _ic_sign_minus_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignMinus16Preview() {
    Icon(
        imageVector = Icons.ic_sign_minus_16,
        contentDescription = null,
    )
}