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

private var _ic_cross_16: ImageVector? = null

val Icons.ic_cross_16: ImageVector
    get() {
        if (_ic_cross_16 != null) return _ic_cross_16!!
        _ic_cross_16 = ImageVector.Builder(
            name = "ic_cross_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.18262 3.1828C3.42671 2.93875 3.82332 2.93873 4.06739 3.1828L8 7.11737L11.9355 3.1828C12.1796 2.93904 12.5754 2.93887 12.8193 3.1828C13.0626 3.42683 13.0629 3.82272 12.8193 4.06659L8.88477 8.00116L12.8164 11.9338C13.0598 12.1779 13.0602 12.5737 12.8164 12.8176C12.5726 13.0614 12.1767 13.0609 11.9326 12.8176L8 8.88494L4.06641 12.8176C3.82244 13.0612 3.42663 13.0611 3.18262 12.8176C2.93903 12.5735 2.93985 12.1777 3.1836 11.9338L7.11621 8.00116L3.18262 4.06756C2.93865 3.82355 2.93879 3.42688 3.18262 3.1828Z"),
            )
        }.build()
        return _ic_cross_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCross16Preview() {
    Icon(
        imageVector = Icons.ic_cross_16,
        contentDescription = null,
    )
}