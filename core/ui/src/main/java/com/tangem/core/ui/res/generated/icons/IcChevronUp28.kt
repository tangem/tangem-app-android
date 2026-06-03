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

private var _ic_chevron_up_28: ImageVector? = null

val Icons.ic_chevron_up_28: ImageVector
    get() {
        if (_ic_chevron_up_28 != null) return _ic_chevron_up_28!!
        _ic_chevron_up_28 = ImageVector.Builder(
            name = "ic_chevron_up_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0009 7.12622C14.3318 7.12643 14.6505 7.25761 14.8847 7.49146L23.6366 16.2434C24.1241 16.7314 24.1241 17.523 23.6366 18.011C23.1486 18.4989 22.3572 18.4986 21.869 18.011L14.0009 10.1438L6.13369 18.011C5.64568 18.4989 4.85427 18.4986 4.36611 18.011C3.87816 17.5228 3.8781 16.7315 4.36611 16.2434L13.1171 7.49146C13.3513 7.25748 13.6698 7.12635 14.0009 7.12622Z"),
            )
        }.build()
        return _ic_chevron_up_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronUp28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_up_28,
        contentDescription = null,
    )
}