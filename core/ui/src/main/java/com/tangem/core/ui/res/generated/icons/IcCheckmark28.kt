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

private var _ic_checkmark_28: ImageVector? = null

val Icons.ic_checkmark_28: ImageVector
    get() {
        if (_ic_checkmark_28 != null) return _ic_checkmark_28!!
        _ic_checkmark_28 = ImageVector.Builder(
            name = "ic_checkmark_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.265 7.29979C21.7431 6.80235 22.5347 6.78687 23.0326 7.26463C23.5302 7.74271 23.5465 8.53429 23.0687 9.03221L11.8665 20.6982C11.6309 20.9437 11.3054 21.0829 10.9652 21.083C10.6249 21.083 10.2995 20.9436 10.0638 20.6982L4.93197 15.3545C4.45426 14.8566 4.46968 14.066 4.96712 13.5879C5.46491 13.1101 6.25655 13.1256 6.7347 13.623L10.9642 18.0273L21.265 7.29979Z"),
            )
        }.build()
        return _ic_checkmark_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCheckmark28Preview() {
    Icon(
        imageVector = Icons.ic_checkmark_28,
        contentDescription = null,
    )
}