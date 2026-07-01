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

private var _ic_sign_minus_20: ImageVector? = null

val Icons.ic_sign_minus_20: ImageVector
    get() {
        if (_ic_sign_minus_20 != null) return _ic_sign_minus_20!!
        _ic_sign_minus_20 = ImageVector.Builder(
            name = "ic_sign_minus_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 9.25C16.4142 9.25 16.75 9.58579 16.75 10C16.75 10.4142 16.4142 10.75 16 10.75H4C3.58579 10.75 3.25 10.4142 3.25 10C3.25 9.58579 3.58579 9.25 4 9.25H16Z"),
            )
        }.build()
        return _ic_sign_minus_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignMinus20Preview() {
    Icon(
        imageVector = Icons.ic_sign_minus_20,
        contentDescription = null,
    )
}