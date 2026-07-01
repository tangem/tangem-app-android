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

private var _ic_chevron_left_20: ImageVector? = null

val Icons.ic_chevron_left_20: ImageVector
    get() {
        if (_ic_chevron_left_20 != null) return _ic_chevron_left_20!!
        _ic_chevron_left_20 = ImageVector.Builder(
            name = "ic_chevron_left_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.7031 3.21976C11.9959 2.92697 12.4708 2.92718 12.7637 3.21976C13.0561 3.51268 13.0564 3.98756 12.7637 4.2803L7.04395 10.002L12.7637 15.7227C13.0561 16.0156 13.0564 16.4905 12.7637 16.7832C12.4709 17.0759 11.996 17.0757 11.7031 16.7832L5.45118 10.5323C5.15861 10.2393 5.15843 9.76352 5.45118 9.47073L11.7031 3.21976Z"),
            )
        }.build()
        return _ic_chevron_left_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronLeft20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_left_20,
        contentDescription = null,
    )
}