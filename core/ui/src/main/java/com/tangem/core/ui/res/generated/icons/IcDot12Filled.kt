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

private var _ic_dot_12_filled: ImageVector? = null

val Icons.ic_dot_12_filled: ImageVector
    get() {
        if (_ic_dot_12_filled != null) return _ic_dot_12_filled!!
        _ic_dot_12_filled = ImageVector.Builder(
            name = "ic_dot_12_filled",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.99976 6C3.99976 7.1047 4.8953 8.00024 6 8.00024C7.1047 8.00024 8.00024 7.1047 8.00024 6C8.00024 4.8953 7.1047 3.99976 6 3.99976C4.8953 3.99976 3.99976 4.8953 3.99976 6Z"),
            )
        }.build()
        return _ic_dot_12_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDot12FilledPreview() {
    Icon(
        imageVector = Icons.ic_dot_12_filled,
        contentDescription = null,
    )
}