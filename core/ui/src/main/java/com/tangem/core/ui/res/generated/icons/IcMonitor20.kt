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

private var _ic_monitor_20: ImageVector? = null

val Icons.ic_monitor_20: ImageVector
    get() {
        if (_ic_monitor_20 != null) return _ic_monitor_20!!
        _ic_monitor_20 = ImageVector.Builder(
            name = "ic_monitor_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.4639 2.99976C15.8646 2.99976 16.9998 4.13456 17 5.53491V11.6863C17 13.0871 15.8654 14.2224 14.4648 14.2224H12.2383L12.3975 15.5007H12.6787C13.0927 15.5009 13.4287 15.8366 13.4287 16.2507C13.4285 16.6646 13.0926 17.0005 12.6787 17.0007H7.32129C6.90721 17.0007 6.57151 16.6648 6.57129 16.2507C6.57129 15.8365 6.90708 15.5007 7.32129 15.5007H7.60254L7.76172 14.2224H5.53516C4.13489 14.2223 3 13.0875 3 11.6873V5.53491C3.00023 4.13484 4.13504 2.99991 5.53516 2.99976H14.4639ZM9.11426 15.5007H10.8857L10.7266 14.2224H9.27344L9.11426 15.5007ZM5.53516 4.49976C4.96357 4.49991 4.50023 4.96316 4.5 5.53491V11.6873C4.5 12.2592 4.96343 12.7223 5.53516 12.7224H14.4648C15.0365 12.7224 15.5 12.2592 15.5 11.6863V5.53491C15.4998 4.96325 15.0365 4.49976 14.4639 4.49976H5.53516Z"),
            )
        }.build()
        return _ic_monitor_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMonitor20Preview() {
    Icon(
        imageVector = Icons.ic_monitor_20,
        contentDescription = null,
    )
}