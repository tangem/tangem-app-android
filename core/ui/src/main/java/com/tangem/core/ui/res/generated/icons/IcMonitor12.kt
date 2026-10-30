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

private var _ic_monitor_12: ImageVector? = null

val Icons.ic_monitor_12: ImageVector
    get() {
        if (_ic_monitor_12 != null) return _ic_monitor_12!!
        _ic_monitor_12 = ImageVector.Builder(
            name = "ic_monitor_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.85645 1.49951C9.76399 1.49951 10.4999 2.23477 10.5 3.14209V7.07861C10.5 7.9861 9.7647 8.72205 8.85742 8.72217H7.45605L7.55273 9.50049H7.71484C7.99088 9.50061 8.21484 9.72442 8.21484 10.0005C8.21469 10.2764 7.99078 10.5004 7.71484 10.5005H4.28613C4.01009 10.5005 3.78629 10.2765 3.78613 10.0005C3.78613 9.72435 4.00999 9.50049 4.28613 9.50049H4.44824L4.54492 8.72217H3.14258C2.23549 8.72213 1.50023 7.98662 1.5 7.07959V3.14209C1.50006 2.23491 2.23539 1.49955 3.14258 1.49951H8.85645ZM5.45605 9.50049H6.54492L6.44824 8.72217H5.55273L5.45605 9.50049ZM3.14258 2.49951C2.78771 2.49955 2.50006 2.78716 2.5 3.14209V7.07959C2.50023 7.43437 2.78781 7.72213 3.14258 7.72217H8.85742C9.21213 7.72205 9.5 7.43411 9.5 7.07861V3.14209C9.49994 2.78727 9.21192 2.49951 8.85645 2.49951H3.14258Z"),
            )
        }.build()
        return _ic_monitor_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMonitor12Preview() {
    Icon(
        imageVector = Icons.ic_monitor_12,
        contentDescription = null,
    )
}