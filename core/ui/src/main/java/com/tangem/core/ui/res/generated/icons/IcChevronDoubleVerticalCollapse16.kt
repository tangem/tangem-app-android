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

private var _ic_chevron_double_vertical_collapse_16: ImageVector? = null

val Icons.ic_chevron_double_vertical_collapse_16: ImageVector
    get() {
        if (_ic_chevron_double_vertical_collapse_16 != null) return _ic_chevron_double_vertical_collapse_16!!
        _ic_chevron_double_vertical_collapse_16 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_collapse_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.9306 13.8101C11.1738 14.0551 11.5694 14.0572 11.8144 13.814C12.0592 13.5708 12.0604 13.1752 11.8173 12.9302L8.44232 9.52984C8.32502 9.41176 8.1654 9.3453 7.99896 9.34527C7.83254 9.34527 7.67292 9.41181 7.5556 9.52984L4.1806 12.9302C3.93746 13.1752 3.93857 13.5709 4.18353 13.814C4.42851 14.0572 4.82416 14.0551 5.06732 13.8101L7.99896 10.857L10.9306 13.8101Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99896 6.64985C8.16534 6.64982 8.32504 6.58327 8.44232 6.46528L11.8173 3.06489C12.0604 2.81999 12.0591 2.42428 11.8144 2.1811C11.5694 1.93795 11.1738 1.94004 10.9306 2.185L7.99896 5.13813L5.06732 2.185C4.82416 1.94004 4.42851 1.93795 4.18353 2.1811C3.9387 2.42427 3.9375 2.81995 4.1806 3.06489L7.5556 6.46528C7.6729 6.58322 7.8326 6.64985 7.99896 6.64985Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_collapse_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVerticalCollapse16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_collapse_16,
        contentDescription = null,
    )
}