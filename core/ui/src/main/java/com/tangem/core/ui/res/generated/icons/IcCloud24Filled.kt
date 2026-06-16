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

private var _ic_cloud_24_filled: ImageVector? = null

val Icons.ic_cloud_24_filled: ImageVector
    get() {
        if (_ic_cloud_24_filled != null) return _ic_cloud_24_filled!!
        _ic_cloud_24_filled = ImageVector.Builder(
            name = "ic_cloud_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 5C15.148 5 17.7001 7.49434 17.7002 10.5713C19.7988 10.5714 21.5 12.2349 21.5 14.2861C21.4998 16.3372 19.7986 17.9999 17.7002 18H7.25C4.62674 18 2.50015 15.9215 2.5 13.3574C2.50423 11.0415 4.25897 9.0845 6.60742 8.77734C7.39237 6.51983 9.56064 5.00117 12 5Z"),
            )
        }.build()
        return _ic_cloud_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud24FilledPreview() {
    Icon(
        imageVector = Icons.ic_cloud_24_filled,
        contentDescription = null,
    )
}