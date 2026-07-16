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

private var _ic_chevron_down_12: ImageVector? = null

val Icons.ic_chevron_down_12: ImageVector
    get() {
        if (_ic_chevron_down_12 != null) return _ic_chevron_down_12!!
        _ic_chevron_down_12 = ImageVector.Builder(
            name = "ic_chevron_down_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.14491 4.39394C9.34016 4.19882 9.65672 4.19878 9.85194 4.39394C10.0465 4.58921 10.0469 4.90591 9.85194 5.10097L6.35097 8.60195C6.25738 8.6954 6.1297 8.74825 5.99745 8.74843C5.86522 8.74837 5.73759 8.69527 5.64394 8.60195L2.14296 5.10097C1.94779 4.90579 1.94796 4.58922 2.14296 4.39394C2.33823 4.19881 2.65477 4.19872 2.84999 4.39394L5.99745 7.5414L9.14491 4.39394Z"),
            )
        }.build()
        return _ic_chevron_down_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDown12Preview() {
    Icon(
        imageVector = Icons.ic_chevron_down_12,
        contentDescription = null,
    )
}