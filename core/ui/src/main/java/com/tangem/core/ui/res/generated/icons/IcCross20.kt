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

private var _ic_cross_20: ImageVector? = null

val Icons.ic_cross_20: ImageVector
    get() {
        if (_ic_cross_20 != null) return _ic_cross_20!!
        _ic_cross_20 = ImageVector.Builder(
            name = "ic_cross_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.71953 3.71917C4.01246 3.42716 4.48748 3.42673 4.78007 3.71917L9.9998 8.94182L15.2195 3.72307C15.5124 3.4307 15.9873 3.4304 16.2801 3.72307C16.5726 4.01582 16.5724 4.49076 16.2801 4.78362L11.0613 10.0024L16.2801 15.2192C16.5727 15.5118 16.5723 15.9868 16.2801 16.2797C15.9874 16.5726 15.5125 16.5731 15.2195 16.2807L9.9998 11.0629L4.78007 16.2836C4.48725 16.5763 4.0124 16.5762 3.71953 16.2836C3.42683 15.9908 3.42682 15.5159 3.71953 15.2231L8.93925 10.0024L3.71953 4.77971C3.42694 4.48677 3.42676 4.01193 3.71953 3.71917Z"),
            )
        }.build()
        return _ic_cross_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCross20Preview() {
    Icon(
        imageVector = Icons.ic_cross_20,
        contentDescription = null,
    )
}