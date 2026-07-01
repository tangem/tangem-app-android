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

private var _ic_sign_plus_20: ImageVector? = null

val Icons.ic_sign_plus_20: ImageVector
    get() {
        if (_ic_sign_plus_20 != null) return _ic_sign_plus_20!!
        _ic_sign_plus_20 = ImageVector.Builder(
            name = "ic_sign_plus_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0049 3.25C10.4191 3.25 10.7549 3.58579 10.7549 4V9.25H16C16.4142 9.25 16.75 9.58579 16.75 10C16.75 10.4142 16.4142 10.75 16 10.75H10.7549V16C10.7549 16.4142 10.4191 16.75 10.0049 16.75C9.59073 16.7499 9.25488 16.4142 9.25488 16V10.75H4C3.58579 10.75 3.25 10.4142 3.25 10C3.25 9.58579 3.58579 9.25 4 9.25H9.25488V4C9.25488 3.58583 9.59073 3.25007 10.0049 3.25Z"),
            )
        }.build()
        return _ic_sign_plus_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignPlus20Preview() {
    Icon(
        imageVector = Icons.ic_sign_plus_20,
        contentDescription = null,
    )
}