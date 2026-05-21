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

private var _ic_sign_equal_28: ImageVector? = null

val Icons.ic_sign_equal_28: ImageVector
    get() {
        if (_ic_sign_equal_28 != null) return _ic_sign_equal_28!!
        _ic_sign_equal_28 = ImageVector.Builder(
            name = "ic_sign_equal_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M22 17.25C22.6904 17.25 23.25 17.8096 23.25 18.5C23.25 19.1904 22.6904 19.75 22 19.75H6C5.30964 19.75 4.75 19.1904 4.75 18.5C4.75 17.8096 5.30964 17.25 6 17.25H22ZM22 8.25C22.6904 8.25 23.25 8.80964 23.25 9.5C23.25 10.1904 22.6904 10.75 22 10.75H6C5.30964 10.75 4.75 10.1904 4.75 9.5C4.75 8.80964 5.30964 8.25 6 8.25H22Z"),
            )
        }.build()
        return _ic_sign_equal_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignEqual28Preview() {
    Icon(
        imageVector = Icons.ic_sign_equal_28,
        contentDescription = null,
    )
}