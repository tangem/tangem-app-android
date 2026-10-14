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

private var _ic_monitor_28: ImageVector? = null

val Icons.ic_monitor_28: ImageVector
    get() {
        if (_ic_monitor_28 != null) return _ic_monitor_28!!
        _ic_monitor_28 = ImageVector.Builder(
            name = "ic_monitor_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.9639 2.99976C23.1933 2.99981 24.9999 4.80599 25 7.03491V16.6306C24.9998 18.86 23.1938 20.6667 20.9648 20.6667H17.583L17.8125 22.5007H18.1787C18.8691 22.5007 19.4287 23.0604 19.4287 23.7507C19.4284 24.4408 18.8689 25.0007 18.1787 25.0007H9.82227C9.13209 25.0007 8.57256 24.4408 8.57227 23.7507C8.57227 23.0604 9.13191 22.5007 9.82227 22.5007H10.1875L10.417 20.6667H7.03516C4.80655 20.6667 3.00009 18.8602 3 16.6316V7.03491C3.00008 4.80631 4.80654 2.99982 7.03516 2.99976H20.9639ZM12.707 22.5007H15.293L15.0635 20.6667H12.9365L12.707 22.5007ZM7.03516 5.49976C6.18729 5.49982 5.50008 6.18699 5.5 7.03491V16.6316C5.50009 17.4795 6.18729 18.1667 7.03516 18.1667H20.9648C21.8124 18.1667 22.4998 17.4799 22.5 16.6306V7.03491C22.4999 6.18731 21.8131 5.49981 20.9639 5.49976H7.03516Z"),
            )
        }.build()
        return _ic_monitor_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMonitor28Preview() {
    Icon(
        imageVector = Icons.ic_monitor_28,
        contentDescription = null,
    )
}