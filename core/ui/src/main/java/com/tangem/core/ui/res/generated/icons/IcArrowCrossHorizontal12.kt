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

private var _ic_arrow_cross_horizontal_12: ImageVector? = null

val Icons.ic_arrow_cross_horizontal_12: ImageVector
    get() {
        if (_ic_arrow_cross_horizontal_12 != null) return _ic_arrow_cross_horizontal_12!!
        _ic_arrow_cross_horizontal_12 = ImageVector.Builder(
            name = "ic_arrow_cross_horizontal_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.87109 6.67676C6.08906 6.50739 6.40278 6.54674 6.57227 6.76466L6.97266 7.27833C7.15245 7.50949 7.41098 7.64245 7.68848 7.64259H8.76074C8.56514 7.45061 8.56036 7.13618 8.75098 6.93848C8.94263 6.73974 9.25922 6.7342 9.45801 6.92579L10.3467 7.78321C10.4443 7.87738 10.4999 8.00693 10.5 8.14259C10.5 8.27835 10.4444 8.4087 10.3467 8.50294L9.45801 9.36036C9.25921 9.55174 8.94256 9.54537 8.75098 9.34669C8.56055 9.14888 8.56496 8.83444 8.76074 8.64259H7.68848C7.07741 8.64245 6.53599 8.34692 6.18262 7.89259L5.7832 7.37794C5.61389 7.15997 5.6532 6.84624 5.87109 6.67676ZM8.75098 2.65333C8.94256 2.45465 9.25921 2.44827 9.45801 2.63966L10.3467 3.49708C10.4444 3.59131 10.5 3.72167 10.5 3.85743C10.4999 3.99309 10.4443 4.12263 10.3467 4.2168L9.45801 5.07423C9.25922 5.26581 8.94263 5.26028 8.75098 5.06153C8.56036 4.86383 8.56514 4.5494 8.76074 4.35743H7.68848C7.41098 4.35756 7.15245 4.49052 6.97266 4.72169L6.9707 4.72266L4.48242 7.89454L4.48145 7.89356C4.12797 8.34711 3.58807 8.64259 2.97754 8.64259H2C1.72386 8.64259 1.5 8.41873 1.5 8.14259C1.50015 7.86657 1.72395 7.64259 2 7.64259H2.97754C3.25519 7.64259 3.51445 7.50961 3.69434 7.27833L3.69531 7.27735L6.18457 4.10548C6.53797 3.6522 7.07825 3.35756 7.68848 3.35743H8.76074C8.56496 3.16557 8.56055 2.85114 8.75098 2.65333ZM2.97754 3.35743C3.58877 3.35743 4.12995 3.653 4.4834 4.10743L4.88379 4.62208C5.05296 4.84003 5.01371 5.15381 4.7959 5.32325C4.57792 5.49278 4.26328 5.45333 4.09375 5.23536L3.69434 4.72169C3.51445 4.49041 3.25519 4.35743 2.97754 4.35743H2C1.72395 4.35743 1.50015 4.13344 1.5 3.85743C1.5 3.58129 1.72386 3.35743 2 3.35743H2.97754Z"),
            )
        }.build()
        return _ic_arrow_cross_horizontal_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCrossHorizontal12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_cross_horizontal_12,
        contentDescription = null,
    )
}