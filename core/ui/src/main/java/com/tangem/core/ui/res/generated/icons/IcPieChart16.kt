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

private var _ic_pie_chart_16: ImageVector? = null

val Icons.ic_pie_chart_16: ImageVector
    get() {
        if (_ic_pie_chart_16 != null) return _ic_pie_chart_16!!
        _ic_pie_chart_16 = ImageVector.Builder(
            name = "ic_pie_chart_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.30664 2.0332C11.4639 2.19304 13.9754 4.80303 13.9756 8C13.9755 9.64424 13.309 11.1315 12.2344 12.2119C12.2306 12.2159 12.2285 12.2217 12.2246 12.2256C12.2205 12.2297 12.2151 12.2324 12.2109 12.2363C11.1985 13.2428 9.82883 13.8906 8.30762 13.9678L8 13.9756L7.69336 13.9678C4.53603 13.808 2.02461 11.197 2.02441 8C2.02463 4.70015 4.70015 2.0256 8 2.02539L8.30664 2.0332ZM7.375 3.31641C5.06079 3.62213 3.27558 5.60224 3.27539 8C3.27559 10.6095 5.3905 12.7254 8 12.7256C9.07912 12.7255 10.0727 12.3619 10.8682 11.7529L7.73242 8.61621C7.61385 8.49763 7.5227 8.35674 7.46191 8.2041C7.45485 8.1862 7.44769 8.16861 7.44238 8.15039C7.39876 8.02387 7.37504 7.8902 7.375 7.75391V3.31641ZM9.01758 8.13379L11.752 10.8691C12.3613 10.0735 12.7255 9.0796 12.7256 8C12.7255 7.36192 12.5975 6.75349 12.3682 6.19824L9.01758 8.13379ZM8.625 6.91797L11.7422 5.11621C10.9983 4.15246 9.89094 3.48481 8.625 3.31738V6.91797Z"),
            )
        }.build()
        return _ic_pie_chart_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPieChart16Preview() {
    Icon(
        imageVector = Icons.ic_pie_chart_16,
        contentDescription = null,
    )
}