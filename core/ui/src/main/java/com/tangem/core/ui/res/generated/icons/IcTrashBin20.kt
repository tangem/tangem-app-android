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

private var _ic_trash_bin_20: ImageVector? = null

val Icons.ic_trash_bin_20: ImageVector
    get() {
        if (_ic_trash_bin_20 != null) return _ic_trash_bin_20!!
        _ic_trash_bin_20 = ImageVector.Builder(
            name = "ic_trash_bin_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.5625 2C12.6456 2 13.4844 2.89922 13.4844 3.95898V4.82031H16.251C16.665 4.82055 17.001 5.15626 17.001 5.57031C17.0008 5.98419 16.6649 6.32008 16.251 6.32031H15.8291V15.6426C15.8289 16.9246 14.8152 18.0046 13.5166 18.0049H6.48438C5.18564 18.0048 4.17204 16.9247 4.17188 15.6426V6.32031H3.75C3.33592 6.32031 3.00022 5.98434 3 5.57031C3.00001 5.15611 3.33579 4.82031 3.75 4.82031H6.51562V3.95898C6.51562 2.89933 7.35451 2.00018 8.4375 2H11.5625ZM5.67188 15.6426C5.67204 16.1403 6.05739 16.5048 6.48438 16.5049H13.5166C13.9434 16.5046 14.3289 16.1401 14.3291 15.6426V6.32031H5.67188V15.6426ZM8.4375 3.5C8.22625 3.50018 8.01562 3.68376 8.01562 3.95898V4.82031H11.9844V3.95898C11.9844 3.68362 11.7739 3.5 11.5625 3.5H8.4375Z"),
            )
        }.build()
        return _ic_trash_bin_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTrashBin20Preview() {
    Icon(
        imageVector = Icons.ic_trash_bin_20,
        contentDescription = null,
    )
}