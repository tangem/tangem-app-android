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

private var _ic_arrow_down_32: ImageVector? = null

val Icons.ic_arrow_down_32: ImageVector
    get() {
        if (_ic_arrow_down_32 != null) return _ic_arrow_down_32!!
        _ic_arrow_down_32 = ImageVector.Builder(
            name = "ic_arrow_down_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.5003 5.33331L14.5003 23.7122L6.39387 15.6058C5.80812 15.0202 4.85852 15.0202 4.27277 15.6058C3.68704 16.1915 3.68715 17.1411 4.27277 17.7269L14.9398 28.3939L15.0491 28.4935C15.3161 28.7123 15.6521 28.8333 16.0003 28.8333C16.398 28.8332 16.7796 28.6751 17.0609 28.3939L27.7269 17.7269C28.3127 17.1411 28.3127 16.1916 27.7269 15.6058C27.1411 15.0203 26.1915 15.0201 25.6058 15.6058L17.5003 23.7122L17.5003 5.33332C17.5003 4.505 16.8286 3.83349 16.0003 3.83332C15.1719 3.83331 14.5003 4.50489 14.5003 5.33331Z"),
            )
        }.build()
        return _ic_arrow_down_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown32Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_32,
        contentDescription = null,
    )
}