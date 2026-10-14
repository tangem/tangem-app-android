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

private var _ic_monitor_24: ImageVector? = null

val Icons.ic_monitor_24: ImageVector
    get() {
        if (_ic_monitor_24 != null) return _ic_monitor_24!!
        _ic_monitor_24 = ImageVector.Builder(
            name = "ic_monitor_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.0781 2.25C19.9742 2.25 21.5115 3.78603 21.5117 5.68164V14.0605C21.5115 15.9566 19.9748 17.4932 18.0791 17.4932H15.0234L15.2471 19.2773H15.6475C16.1994 19.2776 16.6473 19.7254 16.6475 20.2773C16.6472 20.8292 16.1994 21.2771 15.6475 21.2773H8.35059C7.79846 21.2773 7.35085 20.8294 7.35059 20.2773C7.35076 19.7252 7.79841 19.2773 8.35059 19.2773H8.75L8.97266 17.4932H5.91602C4.02079 17.493 2.48456 15.9568 2.48438 14.0615V5.68164C2.48458 3.78642 4.02079 2.25021 5.91602 2.25H18.0781ZM10.7656 19.2773H13.2305L13.0078 17.4932H10.9893L10.7656 19.2773ZM5.91602 4.25C5.12536 4.25021 4.48458 4.89099 4.48438 5.68164V14.0615C4.48456 14.8522 5.12535 15.493 5.91602 15.4932H18.0791C18.8697 15.4932 19.5115 14.8525 19.5117 14.0605V5.68164C19.5115 4.89112 18.8702 4.25 18.0781 4.25H5.91602Z"),
            )
        }.build()
        return _ic_monitor_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMonitor24Preview() {
    Icon(
        imageVector = Icons.ic_monitor_24,
        contentDescription = null,
    )
}