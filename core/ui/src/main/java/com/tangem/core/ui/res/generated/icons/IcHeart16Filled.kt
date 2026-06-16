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

private var _ic_heart_16_filled: ImageVector? = null

val Icons.ic_heart_16_filled: ImageVector
    get() {
        if (_ic_heart_16_filled != null) return _ic_heart_16_filled!!
        _ic_heart_16_filled = ImageVector.Builder(
            name = "ic_heart_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.4668 2.5C12.5799 2.50008 13.9998 4.54887 14 6.45996C13.9996 10.3304 8.10666 13.5 8 13.5C7.89333 13.5 2.0004 10.3304 2 6.45996C2.00021 4.54887 3.4201 2.50008 5.5332 2.5C6.7462 2.5 7.53994 3.12492 8 3.6748C8.46006 3.12492 9.2538 2.5 10.4668 2.5Z"),
            )
        }.build()
        return _ic_heart_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart16FilledPreview() {
    Icon(
        imageVector = Icons.ic_heart_16_filled,
        contentDescription = null,
    )
}