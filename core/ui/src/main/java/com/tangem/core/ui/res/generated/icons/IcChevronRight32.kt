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

private var _ic_chevron_right_32: ImageVector? = null

val Icons.ic_chevron_right_32: ImageVector
    get() {
        if (_ic_chevron_right_32 != null) return _ic_chevron_right_32!!
        _ic_chevron_right_32 = ImageVector.Builder(
            name = "ic_chevron_right_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1076 6.44153C12.6934 5.85575 13.6429 5.85575 14.2287 6.44153L22.7287 14.9415C23.3137 15.5274 23.3141 16.4771 22.7287 17.0626L14.2287 25.5616C13.643 26.1471 12.6933 26.147 12.1076 25.5616C11.5221 24.9759 11.5221 24.0263 12.1076 23.4406L19.5461 16.0011L12.1076 8.56262C11.5219 7.97692 11.5221 7.02733 12.1076 6.44153Z"),
            )
        }.build()
        return _ic_chevron_right_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronRight32Preview() {
    Icon(
        imageVector = Icons.ic_chevron_right_32,
        contentDescription = null,
    )
}