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

private var _ic_logo_tangem_24: ImageVector? = null

val Icons.ic_logo_tangem_24: ImageVector
    get() {
        if (_ic_logo_tangem_24 != null) return _ic_logo_tangem_24!!
        _ic_logo_tangem_24 = ImageVector.Builder(
            name = "ic_logo_tangem_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.66602 11.6797V20H8.31836C7.15663 20 6.57607 19.9997 6.13184 19.793C5.74164 19.6121 5.42574 19.3216 5.22656 18.9648C5.00041 18.5587 5 18.028 5 16.9658V11.6797H9.66602Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19 16.9658C19 18.028 18.9996 18.5587 18.7734 18.9648C18.5743 19.3216 18.2583 19.612 17.8682 19.793C17.4239 19.9997 16.8434 20 15.6816 20H14.334V12.2705L14.333 11.6797H19V16.9658Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.6816 4C16.8434 4 17.4239 4.00026 17.8682 4.20703C18.2584 4.38795 18.5743 4.67839 18.7734 5.03516C18.9996 5.44131 19 5.97203 19 7.03418V8.16016H5V7.03418C5 5.97203 5.00041 5.44131 5.22656 5.03516C5.4244 4.67839 5.74164 4.38917 6.13184 4.20703C6.57607 4.00026 7.15663 4 8.31836 4H15.6816Z"),
            )
        }.build()
        return _ic_logo_tangem_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLogoTangem24Preview() {
    Icon(
        imageVector = Icons.ic_logo_tangem_24,
        contentDescription = null,
    )
}