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

private var _ic_chevron_double_vertical_collapse_32: ImageVector? = null

val Icons.ic_chevron_double_vertical_collapse_32: ImageVector
    get() {
        if (_ic_chevron_double_vertical_collapse_32 != null) return _ic_chevron_double_vertical_collapse_32!!
        _ic_chevron_double_vertical_collapse_32 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_collapse_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.5099 27.5901C22.0567 28.1365 22.9438 28.1367 23.4904 27.5901C24.037 27.0435 24.0367 26.1564 23.4904 25.6097L16.9904 19.1097C16.7279 18.8472 16.3713 18.6996 16.0001 18.6995C15.6288 18.6995 15.2725 18.8471 15.0099 19.1097L8.5099 25.6097C7.96317 26.1564 7.96317 27.0434 8.5099 27.5901C9.05667 28.1365 9.94377 28.1367 10.4904 27.5901L16.0001 22.0804L21.5099 27.5901Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.0001 13.3006C16.3713 13.3005 16.7279 13.1529 16.9904 12.8905L23.4904 6.39048C24.0369 5.84385 24.0368 4.95675 23.4904 4.41002C22.9437 3.8633 22.0566 3.86335 21.5099 4.41002L16.0001 9.91978L10.4904 4.41002C9.94366 3.86331 9.05664 3.86335 8.5099 4.41002C7.96317 4.95675 7.96317 5.84375 8.5099 6.39048L15.0099 12.8905C15.2724 13.1528 15.629 13.3006 16.0001 13.3006Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_collapse_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVerticalCollapse32Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_collapse_32,
        contentDescription = null,
    )
}