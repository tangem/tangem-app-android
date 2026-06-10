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

private var _ic_sign_equal_32: ImageVector? = null

val Icons.ic_sign_equal_32: ImageVector
    get() {
        if (_ic_sign_equal_32 != null) return _ic_sign_equal_32!!
        _ic_sign_equal_32 = ImageVector.Builder(
            name = "ic_sign_equal_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M25.5 20C26.3284 20 27 20.6716 27 21.5C27 22.3284 26.3284 23 25.5 23H6.5C5.67157 23 5 22.3284 5 21.5C5 20.6716 5.67157 20 6.5 20H25.5ZM25.5 9C26.3284 9 27 9.67157 27 10.5C27 11.3284 26.3284 12 25.5 12H6.5C5.67157 12 5 11.3284 5 10.5C5 9.67157 5.67157 9 6.5 9H25.5Z"),
            )
        }.build()
        return _ic_sign_equal_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignEqual32Preview() {
    Icon(
        imageVector = Icons.ic_sign_equal_32,
        contentDescription = null,
    )
}