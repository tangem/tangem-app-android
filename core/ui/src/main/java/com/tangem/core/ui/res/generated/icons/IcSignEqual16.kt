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
                pathData = addPathNodes("M12.5 9.875C12.8452 9.875 13.125 10.1548 13.125 10.5C13.125 10.8452 12.8452 11.125 12.5 11.125H3.5C3.15482 11.125 2.875 10.8452 2.875 10.5C2.875 10.1548 3.15482 9.875 3.5 9.875H12.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5 4.875C12.8452 4.875 13.125 5.15482 13.125 5.5C13.125 5.84518 12.8452 6.125 12.5 6.125H3.5C3.15482 6.125 2.875 5.84518 2.875 5.5C2.875 5.15482 3.15482 4.875 3.5 4.875H12.5Z"),
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