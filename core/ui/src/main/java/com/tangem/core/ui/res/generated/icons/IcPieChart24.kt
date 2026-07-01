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

private var _ic_pie_chart_24: ImageVector? = null

val Icons.ic_pie_chart_24: ImageVector
    get() {
        if (_ic_pie_chart_24 != null) return _ic_pie_chart_24!!
        _ic_pie_chart_24 = ImageVector.Builder(
            name = "ic_pie_chart_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5233 2 22 6.47672 22 12C22 17.5233 17.5233 22 12 22C6.47672 22 2 17.5233 2 12C2 6.47672 6.47672 2 12 2ZM11 4.06348C7.05345 4.55557 4 7.92002 4 12C4 16.4187 7.58128 20 12 20C13.8488 20 15.5497 19.3713 16.9043 18.3184L11.5859 13C11.4248 12.8389 11.296 12.6524 11.1992 12.4512C11.1566 12.3756 11.1249 12.297 11.1035 12.2168C11.0364 12.0151 11 11.8025 11 11.5859V4.06348ZM13.6279 12.2139L18.3184 16.9043C19.3713 15.5497 20 13.8488 20 12C20 10.8985 19.7766 9.84944 19.374 8.89453L13.6279 12.2139ZM13 10.2676L18.3721 7.16309C17.1013 5.49147 15.1881 4.33631 13 4.06348V10.2676Z"),
            )
        }.build()
        return _ic_pie_chart_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPieChart24Preview() {
    Icon(
        imageVector = Icons.ic_pie_chart_24,
        contentDescription = null,
    )
}