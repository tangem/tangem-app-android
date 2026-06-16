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

private var _ic_sign_equal_16: ImageVector? = null

val Icons.ic_sign_equal_16: ImageVector
    get() {
        if (_ic_sign_equal_16 != null) return _ic_sign_equal_16!!
        _ic_sign_equal_16 = ImageVector.Builder(
            name = "ic_sign_equal_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5 10C12.7761 10 13 10.2239 13 10.5C13 10.7761 12.7761 11 12.5 11H3.5C3.22386 11 3 10.7761 3 10.5C3 10.2239 3.22386 10 3.5 10H12.5ZM12.5 5C12.7761 5 13 5.22386 13 5.5C13 5.77614 12.7761 6 12.5 6H3.5C3.22386 6 3 5.77614 3 5.5C3 5.22386 3.22386 5 3.5 5H12.5Z"),
            )
        }.build()
        return _ic_sign_equal_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignEqual16Preview() {
    Icon(
        imageVector = Icons.ic_sign_equal_16,
        contentDescription = null,
    )
}