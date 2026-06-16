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

private var _ic_heart_20_filled: ImageVector? = null

val Icons.ic_heart_20_filled: ImageVector
    get() {
        if (_ic_heart_20_filled != null) return _ic_heart_20_filled!!
        _ic_heart_20_filled = ImageVector.Builder(
            name = "ic_heart_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.083 3.5C15.7246 3.5 17.4998 5.92103 17.5 8.17969C17.5 12.7541 10.1333 16.5 10 16.5C9.86667 16.5 2.5 12.7541 2.5 8.17969C2.50016 5.92103 4.27545 3.5 6.91699 3.5C8.43348 3.50011 9.42504 4.23969 10 4.88965C10.575 4.23969 11.5665 3.50011 13.083 3.5Z"),
            )
        }.build()
        return _ic_heart_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart20FilledPreview() {
    Icon(
        imageVector = Icons.ic_heart_20_filled,
        contentDescription = null,
    )
}