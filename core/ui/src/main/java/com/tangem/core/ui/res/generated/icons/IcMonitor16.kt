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

private var _ic_monitor_16: ImageVector? = null

val Icons.ic_monitor_16: ImageVector
    get() {
        if (_ic_monitor_16 != null) return _ic_monitor_16!!
        _ic_monitor_16 = ImageVector.Builder(
            name = "ic_monitor_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.125 2.99902C12.1607 2.99927 13 3.83858 13 4.87402V9.18066C12.9999 10.2164 12.1607 11.0557 11.125 11.0557H9.68066L9.7666 11.751H9.87598C10.2208 11.7514 10.501 12.031 10.501 12.376C10.5009 12.7209 10.2208 13.0006 9.87598 13.001H6.12598C5.78082 13.001 5.50101 12.7211 5.50098 12.376C5.50098 12.0308 5.7808 11.751 6.12598 11.751H6.2334L6.32031 11.0557H4.875C3.83942 11.0557 3 10.2161 3 9.18066V4.87402C3.00003 3.83859 3.83944 2.99902 4.875 2.99902H11.125ZM7.49316 11.751H8.50684L8.41992 11.0557H7.58008L7.49316 11.751ZM4.875 4.24902C4.53008 4.24902 4.25003 4.52866 4.25 4.87402V9.18066C4.25 9.52605 4.53006 9.80566 4.875 9.80566H11.125C11.4697 9.80566 11.7499 9.52661 11.75 9.18066V4.87402C11.75 4.52897 11.4704 4.24927 11.125 4.24902H4.875Z"),
            )
        }.build()
        return _ic_monitor_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMonitor16Preview() {
    Icon(
        imageVector = Icons.ic_monitor_16,
        contentDescription = null,
    )
}