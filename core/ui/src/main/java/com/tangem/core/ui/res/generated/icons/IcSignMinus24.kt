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

private var _ic_sign_minus_24: ImageVector? = null

val Icons.ic_sign_minus_24: ImageVector
    get() {
        if (_ic_sign_minus_24 != null) return _ic_sign_minus_24!!
        _ic_sign_minus_24 = ImageVector.Builder(
            name = "ic_sign_minus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19 11C19.5523 11 20 11.4477 20 12C20 12.5523 19.5523 13 19 13H5C4.44772 13 4 12.5523 4 12C4 11.4477 4.44772 11 5 11H19Z"),
            )
        }.build()
        return _ic_sign_minus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignMinus24Preview() {
    Icon(
        imageVector = Icons.ic_sign_minus_24,
        contentDescription = null,
    )
}