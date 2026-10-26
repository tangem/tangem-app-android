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

private var _ic_chart_ascending_12: ImageVector? = null

val Icons.ic_chart_ascending_12: ImageVector
    get() {
        if (_ic_chart_ascending_12 != null) return _ic_chart_ascending_12!!
        _ic_chart_ascending_12 = ImageVector.Builder(
            name = "ic_chart_ascending_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.00024 2C2.27639 2 2.50024 2.22386 2.50024 2.5V9H9.99927C10.2754 9 10.4993 9.22386 10.4993 9.5C10.4993 9.77614 10.2754 10 9.99927 10H2.00024C1.7241 10 1.50024 9.77614 1.50024 9.5V2.5C1.50024 2.22386 1.7241 2 2.00024 2Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0676 3.5C10.0801 3.5017 10.0926 3.50229 10.1047 3.50488C10.122 3.50858 10.1386 3.51497 10.1555 3.52051C10.1696 3.52513 10.184 3.52837 10.1975 3.53418C10.2081 3.5387 10.2175 3.5455 10.2278 3.55078C10.2472 3.5608 10.2666 3.57062 10.2844 3.58301C10.2886 3.5859 10.292 3.58975 10.2961 3.59277C10.3453 3.62916 10.3872 3.67394 10.4202 3.72559C10.4276 3.73724 10.4333 3.74963 10.4397 3.76172C10.4475 3.77653 10.4558 3.79099 10.4622 3.80664C10.4698 3.82551 10.4755 3.84478 10.4807 3.86426C10.484 3.87658 10.4881 3.88869 10.4905 3.90137C10.4936 3.91784 10.4939 3.93453 10.4954 3.95117C10.4966 3.96525 10.5002 3.97976 10.5002 3.99414V5.04883C10.4998 5.32445 10.2759 5.54856 10.0002 5.54883C9.75663 5.54862 9.55329 5.37341 9.50903 5.14258L7.84302 6.71387L7.84204 6.71289C7.4855 7.04902 6.91978 7.04941 6.56372 6.71387L5.71216 5.91016L3.63208 7.87207C3.43111 8.06094 3.11428 8.05134 2.92505 7.85059C2.73621 7.64977 2.74616 7.33385 2.94653 7.14453L5.07251 5.13965L5.14185 5.08008C5.49897 4.80453 6.01855 4.82385 6.35278 5.13965L7.20239 5.94238L8.75806 4.47559C8.54214 4.4204 8.3823 4.2272 8.38208 3.99414C8.38208 3.718 8.60594 3.49414 8.88208 3.49414H10.0149C10.0326 3.49468 10.0501 3.49759 10.0676 3.5Z"),
            )
        }.build()
        return _ic_chart_ascending_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartAscending12Preview() {
    Icon(
        imageVector = Icons.ic_chart_ascending_12,
        contentDescription = null,
    )
}