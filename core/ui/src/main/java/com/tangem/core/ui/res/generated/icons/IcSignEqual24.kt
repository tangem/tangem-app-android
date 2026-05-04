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

private var _ic_sign_equal_24: ImageVector? = null

val Icons.ic_sign_equal_24: ImageVector
    get() {
        if (_ic_sign_equal_24 != null) return _ic_sign_equal_24!!
        _ic_sign_equal_24 = ImageVector.Builder(
            name = "ic_sign_equal_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19 15C19.5523 15 20 15.4477 20 16C20 16.5523 19.5523 17 19 17H5C4.44772 17 4 16.5523 4 16C4 15.4477 4.44772 15 5 15H19ZM19 7C19.5523 7 20 7.44772 20 8C20 8.55228 19.5523 9 19 9H5C4.44772 9 4 8.55228 4 8C4 7.44772 4.44772 7 5 7H19Z"),
            )
        }.build()
        return _ic_sign_equal_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignEqual24Preview() {
    Icon(
        imageVector = Icons.ic_sign_equal_24,
        contentDescription = null,
    )
}