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

private var _ic_chevron_right_20: ImageVector? = null

val Icons.ic_chevron_right_20: ImageVector
    get() {
        if (_ic_chevron_right_20 != null) return _ic_chevron_right_20!!
        _ic_chevron_right_20 = ImageVector.Builder(
            name = "ic_chevron_right_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.23735 3.2178C7.53027 2.92522 8.00511 2.92502 8.2979 3.2178L14.5499 9.46878C14.8423 9.7616 14.8423 10.2375 14.5499 10.5303L8.2979 16.7813C8.00512 17.074 7.53026 17.0738 7.23735 16.7813C6.94448 16.4884 6.9445 16.0136 7.23735 15.7207L12.9571 10L7.23735 4.27835C6.94449 3.98548 6.94454 3.5107 7.23735 3.2178Z"),
            )
        }.build()
        return _ic_chevron_right_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronRight20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_right_20,
        contentDescription = null,
    )
}