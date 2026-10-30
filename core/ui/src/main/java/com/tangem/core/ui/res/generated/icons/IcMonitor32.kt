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

private var _ic_monitor_32: ImageVector? = null

val Icons.ic_monitor_32: ImageVector
    get() {
        if (_ic_monitor_32 != null) return _ic_monitor_32!!
        _ic_monitor_32 = ImageVector.Builder(
            name = "ic_monitor_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.2139 3C26.8575 3.00014 29 5.14199 29 7.78516V19.1025C28.9998 21.7461 26.8579 23.8887 24.2148 23.8887H20.2549L20.5186 26H20.9287C21.7571 26 22.4287 26.6716 22.4287 27.5C22.4284 28.3282 21.757 29 20.9287 29H11.0723C10.244 29 9.57254 28.3282 9.57227 27.5C9.57227 26.6716 10.2438 26 11.0723 26H11.4814L11.7451 23.8887H7.78516C5.14235 23.8887 3.00005 21.7463 3 19.1035V7.78516C3 5.1423 5.14232 3.00001 7.78516 3H24.2139ZM14.5049 26H17.4951L17.2314 23.8887H14.7686L14.5049 26ZM7.78516 6C6.79911 6.00001 6 6.79921 6 7.78516V19.1035C6.00005 20.0894 6.79914 20.8887 7.78516 20.8887H24.2148C25.2004 20.8887 25.9998 20.09 26 19.1025V7.78516C26 6.79967 25.2015 6.00014 24.2139 6H7.78516Z"),
            )
        }.build()
        return _ic_monitor_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMonitor32Preview() {
    Icon(
        imageVector = Icons.ic_monitor_32,
        contentDescription = null,
    )
}