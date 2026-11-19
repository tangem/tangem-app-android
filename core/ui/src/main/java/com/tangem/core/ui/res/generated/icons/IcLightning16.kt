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

private var _ic_lightning_16: ImageVector? = null

val Icons.ic_lightning_16: ImageVector
    get() {
        if (_ic_lightning_16 != null) return _ic_lightning_16!!
        _ic_lightning_16 = ImageVector.Builder(
            name = "ic_lightning_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.01551 2.41787C7.39331 2.00906 7.94206 1.93142 8.37195 2.05069C8.83093 2.17808 9.31238 2.58354 9.31238 3.23623V6.15322H12.1864C12.6778 6.15322 13.1067 6.40268 13.3339 6.78799C13.5664 7.18244 13.5709 7.70216 13.245 8.121L9.05848 13.495C8.68117 13.9787 8.0863 14.0764 7.62781 13.9491C7.16896 13.8217 6.68849 13.416 6.68836 12.7636V9.84658H3.81336C3.32199 9.84657 2.89323 9.59644 2.6659 9.21084C2.43329 8.81599 2.42931 8.29637 2.75574 7.87783L6.94226 2.50381L7.01551 2.41787ZM7.95789 3.25283L7.92762 3.27237L3.78504 8.58975C3.79315 8.59115 3.80207 8.59658 3.81336 8.59658H7.31336C7.65841 8.59673 7.93836 8.8765 7.93836 9.22158V12.7323C7.94449 12.7354 7.95195 12.741 7.96277 12.744C7.99281 12.7523 8.02189 12.7515 8.04187 12.746C8.05778 12.7416 8.06515 12.7354 8.07215 12.7265L12.2137 7.40908C12.206 7.40782 12.197 7.40322 12.1864 7.40322H8.68738C8.34223 7.40322 8.06242 7.12337 8.06238 6.77822V3.26553C8.05628 3.26248 8.04865 3.25776 8.03797 3.25479C8.00783 3.24642 7.97793 3.24732 7.95789 3.25283Z"),
            )
        }.build()
        return _ic_lightning_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLightning16Preview() {
    Icon(
        imageVector = Icons.ic_lightning_16,
        contentDescription = null,
    )
}