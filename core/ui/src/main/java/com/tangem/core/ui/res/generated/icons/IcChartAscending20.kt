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

private var _ic_chart_ascending_20: ImageVector? = null

val Icons.ic_chart_ascending_20: ImageVector
    get() {
        if (_ic_chart_ascending_20 != null) return _ic_chart_ascending_20!!
        _ic_chart_ascending_20 = ImageVector.Builder(
            name = "ic_chart_ascending_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.75 3.5C4.16421 3.5 4.5 3.83579 4.5 4.25V15H16.249C16.6632 15 16.999 15.3358 16.999 15.75C16.999 16.1642 16.6632 16.5 16.249 16.5H3.75C3.33579 16.5 3 16.1642 3 15.75V4.25C3 3.83579 3.33579 3.5 3.75 3.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.2549 5.9541C16.3332 5.95462 16.4076 5.97038 16.4785 5.99316C16.4976 5.9993 16.5175 6.00304 16.5361 6.01074C16.5639 6.02222 16.5893 6.03813 16.6152 6.05273C16.6744 6.08603 16.732 6.12522 16.7822 6.17578C16.8112 6.20507 16.8362 6.23701 16.8594 6.26953C16.9471 6.3923 17 6.54173 17 6.7041V8.43652C16.9999 8.85057 16.664 9.1864 16.25 9.18652C15.8569 9.18652 15.5348 8.88391 15.5029 8.49902L12.8721 11.1055L12.873 11.1064C12.3247 11.651 11.4378 11.6494 10.8896 11.1064L9.55078 9.7793L6.29395 13.0098C5.99992 13.3012 5.52502 13.2986 5.2334 13.0049C4.94192 12.7108 4.94339 12.2359 5.2373 11.9443L8.56055 8.65137L8.66699 8.55566C9.21808 8.10993 10.0298 8.1405 10.5439 8.65137L11.8809 9.97656L14.4297 7.4502C14.0494 7.41382 13.752 7.09392 13.752 6.7041C13.752 6.28989 14.0877 5.9541 14.502 5.9541H16.2549Z"),
            )
        }.build()
        return _ic_chart_ascending_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartAscending20Preview() {
    Icon(
        imageVector = Icons.ic_chart_ascending_20,
        contentDescription = null,
    )
}