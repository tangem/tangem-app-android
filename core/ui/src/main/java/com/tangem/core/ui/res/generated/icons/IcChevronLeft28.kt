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

private var _ic_chevron_left_28: ImageVector? = null

val Icons.ic_chevron_left_28: ImageVector
    get() {
        if (_ic_chevron_left_28 != null) return _ic_chevron_left_28!!
        _ic_chevron_left_28 = ImageVector.Builder(
            name = "ic_chevron_left_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.2422 4.36594C16.7301 3.87816 17.5216 3.87853 18.0098 4.36594C18.4979 4.85404 18.4978 5.64535 18.0098 6.13352L10.1426 14.0017L18.0098 21.8689C18.4979 22.357 18.4978 23.1483 18.0098 23.6364C17.5216 24.1244 16.7303 24.1245 16.2422 23.6364L7.49023 14.8855C7.25637 14.6512 7.12513 14.3327 7.125 14.0017C7.12515 13.6707 7.25638 13.3521 7.49023 13.1179L16.2422 4.36594Z"),
            )
        }.build()
        return _ic_chevron_left_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronLeft28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_left_28,
        contentDescription = null,
    )
}