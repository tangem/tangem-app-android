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

private var _ic_heart_24_filled: ImageVector? = null

val Icons.ic_heart_24_filled: ImageVector
    get() {
        if (_ic_heart_24_filled != null) return _ic_heart_24_filled!!
        _ic_heart_24_filled = ImageVector.Builder(
            name = "ic_heart_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.1113 3C19.6334 3.00014 22 6.35303 22 9.48047C21.9995 15.814 12.1778 21 12 21C11.8222 21 2.00048 15.814 2 9.48047C2 6.35303 4.36657 3.00014 7.88867 3C9.91089 3 11.2333 4.02383 12 4.92383C12.7667 4.02383 14.0891 3 16.1113 3Z"),
            )
        }.build()
        return _ic_heart_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart24FilledPreview() {
    Icon(
        imageVector = Icons.ic_heart_24_filled,
        contentDescription = null,
    )
}