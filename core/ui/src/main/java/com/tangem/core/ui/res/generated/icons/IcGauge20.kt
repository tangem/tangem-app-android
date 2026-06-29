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

private var _ic_gauge_20: ImageVector? = null

val Icons.ic_gauge_20: ImageVector
    get() {
        if (_ic_gauge_20 != null) return _ic_gauge_20!!
        _ic_gauge_20 = ImageVector.Builder(
            name = "ic_gauge_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.34068 4.36703C7.46544 1.21069 12.5353 1.21064 15.66 4.36703C18.78 7.51949 18.7798 12.6275 15.66 15.7801C15.3686 16.0744 14.8928 16.0773 14.5985 15.786C14.3047 15.4946 14.3027 15.0197 14.5936 14.7254C17.1352 12.1572 17.1355 7.98972 14.5936 5.42172C12.0559 2.85837 7.94386 2.85841 5.40611 5.42172C2.86441 7.98976 2.86452 12.1573 5.40611 14.7254C5.6973 15.0197 5.69514 15.4946 5.40123 15.786C5.10686 16.0772 4.63204 16.0744 4.34068 15.7801C1.22049 12.6275 1.22035 7.51959 4.34068 4.36703Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1219 6.72445C12.4133 6.43067 12.8883 6.42759 13.1825 6.71859C13.4765 7.00977 13.479 7.48477 13.1883 7.77914L10.4637 10.5311C10.1723 10.8253 9.69749 10.8273 9.40318 10.536C9.1089 10.2446 9.10599 9.76976 9.39732 9.47543L12.1219 6.72445Z"),
            )
        }.build()
        return _ic_gauge_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGauge20Preview() {
    Icon(
        imageVector = Icons.ic_gauge_20,
        contentDescription = null,
    )
}