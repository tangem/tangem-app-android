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

private var _ic_cloud_20_filled: ImageVector? = null

val Icons.ic_cloud_20_filled: ImageVector
    get() {
        if (_ic_cloud_20_filled != null) return _ic_cloud_20_filled!!
        _ic_cloud_20_filled = ImageVector.Builder(
            name = "ic_cloud_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 4.5C12.6508 4.5 14.7996 6.61043 14.7998 9.21387C16.5671 9.21387 18 10.6217 18 12.3574C17.9998 14.093 16.567 15.5 14.7998 15.5H6C3.79086 15.5 2 13.741 2 11.5713C2.00364 9.61167 3.48134 7.95614 5.45898 7.69629C6.11998 5.78607 7.94579 4.50099 10 4.5Z"),
            )
        }.build()
        return _ic_cloud_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud20FilledPreview() {
    Icon(
        imageVector = Icons.ic_cloud_20_filled,
        contentDescription = null,
    )
}