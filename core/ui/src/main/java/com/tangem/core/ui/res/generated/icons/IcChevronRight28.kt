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

private var _ic_chevron_right_28: ImageVector? = null

val Icons.ic_chevron_right_28: ImageVector
    get() {
        if (_ic_chevron_right_28 != null) return _ic_chevron_right_28!!
        _ic_chevron_right_28 = ImageVector.Builder(
            name = "ic_chevron_right_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99008 4.36269C10.4783 3.87543 11.2698 3.87497 11.7577 4.36269L20.5096 13.1146C20.7434 13.3488 20.8746 13.6675 20.8748 13.9984C20.8747 14.3294 20.7434 14.648 20.5096 14.8822L11.7577 23.6332C11.2696 24.1213 10.4782 24.1212 9.99008 23.6332C9.502 23.145 9.50195 22.3537 9.99008 21.8656L17.8573 13.9984L9.99008 6.13027C9.50207 5.6421 9.50197 4.8508 9.99008 4.36269Z"),
            )
        }.build()
        return _ic_chevron_right_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronRight28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_right_28,
        contentDescription = null,
    )
}