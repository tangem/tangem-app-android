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

private var _ic_sign_equal_20: ImageVector? = null

val Icons.ic_sign_equal_20: ImageVector
    get() {
        if (_ic_sign_equal_20 != null) return _ic_sign_equal_20!!
        _ic_sign_equal_20 = ImageVector.Builder(
            name = "ic_sign_equal_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 12.75C16.4142 12.75 16.75 13.0858 16.75 13.5C16.75 13.9142 16.4142 14.25 16 14.25H4C3.58579 14.25 3.25 13.9142 3.25 13.5C3.25 13.0858 3.58579 12.75 4 12.75H16ZM16 5.75C16.4142 5.75 16.75 6.08579 16.75 6.5C16.75 6.91421 16.4142 7.25 16 7.25H4C3.58579 7.25 3.25 6.91421 3.25 6.5C3.25 6.08579 3.58579 5.75 4 5.75H16Z"),
            )
        }.build()
        return _ic_sign_equal_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignEqual20Preview() {
    Icon(
        imageVector = Icons.ic_sign_equal_20,
        contentDescription = null,
    )
}