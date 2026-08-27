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

private var _ic_arrow_down_right_16: ImageVector? = null

val Icons.ic_arrow_down_right_16: ImageVector
    get() {
        if (_ic_arrow_down_right_16 != null) return _ic_arrow_down_right_16!!
        _ic_arrow_down_right_16 = ImageVector.Builder(
            name = "ic_arrow_down_right_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.3513 12.9766C12.5168 12.9766 12.6765 12.9109 12.7936 12.794C12.9106 12.6769 12.9762 12.5171 12.9763 12.3516V6.25005C12.976 5.90505 12.6963 5.62505 12.3513 5.62505C12.0064 5.62532 11.7265 5.90522 11.7263 6.25005V10.8428L4.09149 3.20805C3.84746 2.96438 3.45169 2.9643 3.2077 3.20805C2.9641 3.45215 2.96384 3.84889 3.2077 4.09282L10.8415 11.7266H6.24969C5.9049 11.7269 5.62494 12.0068 5.62469 12.3516C5.62491 12.6964 5.90489 12.9763 6.24969 12.9766H12.3513Z"),
            )
        }.build()
        return _ic_arrow_down_right_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownRight16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_right_16,
        contentDescription = null,
    )
}