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

private var _ic_arrow_up_16: ImageVector? = null

val Icons.ic_arrow_up_16: ImageVector
    get() {
        if (_ic_arrow_up_16 != null) return _ic_arrow_up_16!!
        _ic_arrow_up_16 = ImageVector.Builder(
            name = "ic_arrow_up_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.1851 7.25277C2.94168 7.00873 2.94149 6.61289 3.1851 6.36898L7.55815 1.99593C7.80205 1.75241 8.19793 1.75258 8.44194 1.99593L12.815 6.36898C13.0585 6.61298 13.0586 7.00881 12.815 7.25277C12.571 7.49666 12.1743 7.49659 11.9302 7.25277L8.62456 3.9471V13.4998C8.62445 13.8448 8.34455 14.1247 7.99956 14.1248C7.65486 14.1244 7.37466 13.8446 7.37456 13.4998V3.94808L4.06889 7.25277C3.82486 7.4965 3.42911 7.49654 3.1851 7.25277Z"),
            )
        }.build()
        return _ic_arrow_up_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_16,
        contentDescription = null,
    )
}