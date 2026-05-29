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

private var _ic_chevron_up_16: ImageVector? = null

val Icons.ic_chevron_up_16: ImageVector
    get() {
        if (_ic_chevron_up_16 != null) return _ic_chevron_up_16!!
        _ic_chevron_up_16 = ImageVector.Builder(
            name = "ic_chevron_up_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99736 4.56152C8.16301 4.56161 8.3226 4.62798 8.43974 4.74512L12.8128 9.11719C13.0563 9.36127 13.0566 9.75803 12.8128 10.002C12.5688 10.2456 12.172 10.2454 11.928 10.002L7.99736 6.07129L4.06767 10.002C3.82374 10.2456 3.42692 10.2454 3.1829 10.002C2.93893 9.75798 2.93913 9.36129 3.1829 9.11719L7.55595 4.74512C7.67304 4.62809 7.83182 4.56165 7.99736 4.56152Z"),
            )
        }.build()
        return _ic_chevron_up_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronUp16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_up_16,
        contentDescription = null,
    )
}