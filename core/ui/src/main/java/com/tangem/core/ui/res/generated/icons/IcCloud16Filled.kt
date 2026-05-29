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

private var _ic_cloud_16_filled: ImageVector? = null

val Icons.ic_cloud_16_filled: ImageVector
    get() {
        if (_ic_cloud_16_filled != null) return _ic_cloud_16_filled!!
        _ic_cloud_16_filled = ImageVector.Builder(
            name = "ic_cloud_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 4C9.98822 4 11.5996 5.53516 11.5996 7.42871C12.9249 7.42871 13.9998 8.45169 14 9.71387C14 10.9762 12.9251 12 11.5996 12H5C3.34315 12 2 10.7205 2 9.14258C2.00282 7.71747 3.11154 6.51318 4.59473 6.32422C5.09062 4.93522 6.45951 4.00072 8 4Z"),
            )
        }.build()
        return _ic_cloud_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud16FilledPreview() {
    Icon(
        imageVector = Icons.ic_cloud_16_filled,
        contentDescription = null,
    )
}