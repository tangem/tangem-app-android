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

private var _ic_arrow_top_right_16: ImageVector? = null

val Icons.ic_arrow_top_right_16: ImageVector
    get() {
        if (_ic_arrow_top_right_16 != null) return _ic_arrow_top_right_16!!
        _ic_arrow_top_right_16 = ImageVector.Builder(
            name = "ic_arrow_top_right_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.3513 3.02344C12.5169 3.02346 12.6766 3.08911 12.7937 3.20605C12.9106 3.32314 12.9762 3.48295 12.9763 3.64844V9.75C12.9761 10.095 12.6963 10.3749 12.3513 10.375C12.0064 10.3748 11.7265 10.0949 11.7263 9.75V5.15723L4.09155 12.792C3.84754 13.0357 3.45177 13.0357 3.20777 12.792C2.96408 12.5479 2.96382 12.1512 3.20777 11.9072L10.8416 4.27344H6.24976C5.90491 4.27323 5.625 3.99328 5.62476 3.64844C5.62497 3.30357 5.90489 3.02364 6.24976 3.02344H12.3513Z"),
            )
        }.build()
        return _ic_arrow_top_right_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopRight16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_right_16,
        contentDescription = null,
    )
}