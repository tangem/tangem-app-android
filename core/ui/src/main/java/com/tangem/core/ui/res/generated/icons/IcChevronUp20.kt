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

private var _ic_chevron_up_20: ImageVector? = null

val Icons.ic_chevron_up_20: ImageVector
    get() {
        if (_ic_chevron_up_20 != null) return _ic_chevron_up_20!!
        _ic_chevron_up_20 = ImageVector.Builder(
            name = "ic_chevron_up_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.001 5.23242C10.1997 5.23259 10.3917 5.31165 10.5322 5.45215L16.7832 11.7041C17.0756 11.9969 17.0757 12.4719 16.7832 12.7646C16.4904 13.0574 16.0156 13.0572 15.7227 12.7646L10.001 7.04395L4.28027 12.7646C3.9875 13.0574 3.51264 13.0572 3.21972 12.7646C2.92684 12.4718 2.92684 11.997 3.21972 11.7041L9.4707 5.45215C9.61122 5.31178 9.80236 5.23254 10.001 5.23242Z"),
            )
        }.build()
        return _ic_chevron_up_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronUp20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_up_20,
        contentDescription = null,
    )
}