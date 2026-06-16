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

private var _ic_heart_32_filled: ImageVector? = null

val Icons.ic_heart_32_filled: ImageVector
    get() {
        if (_ic_heart_32_filled != null) return _ic_heart_32_filled!!
        _ic_heart_32_filled = ImageVector.Builder(
            name = "ic_heart_32_filled",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.3447 4.5C25.9235 4.50018 29 8.78411 29 12.7803C28.9997 20.8733 16.2311 27.5 16 27.5C15.7689 27.5 3.00029 20.8733 3 12.7803C3 8.78411 6.07654 4.50018 10.6553 4.5C13.2841 4.5 15.0033 5.80802 16 6.95801C16.9967 5.80802 18.7159 4.5 21.3447 4.5Z"),
            )
        }.build()
        return _ic_heart_32_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart32FilledPreview() {
    Icon(
        imageVector = Icons.ic_heart_32_filled,
        contentDescription = null,
    )
}