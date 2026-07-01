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

private var _ic_chevron_down_32: ImageVector? = null

val Icons.ic_chevron_down_32: ImageVector
    get() {
        if (_ic_chevron_down_32 != null) return _ic_chevron_down_32!!
        _ic_chevron_down_32 = ImageVector.Builder(
            name = "ic_chevron_down_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.4403 12.1087C24.0261 11.5231 24.9757 11.523 25.5614 12.1087C26.1467 12.6944 26.1468 13.6441 25.5614 14.2297L17.0624 22.7297C16.7812 23.0109 16.3985 23.1691 16.0009 23.1692C15.6034 23.1691 15.2215 23.0107 14.9403 22.7297L6.44129 14.2297C5.85558 13.644 5.85568 12.6944 6.44129 12.1087C7.0271 11.5231 7.97668 11.5229 8.56239 12.1087L16.0009 19.5471L23.4403 12.1087Z"),
            )
        }.build()
        return _ic_chevron_down_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDown32Preview() {
    Icon(
        imageVector = Icons.ic_chevron_down_32,
        contentDescription = null,
    )
}