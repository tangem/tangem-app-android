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

private var _ic_chart_line_vertical_28: ImageVector? = null

val Icons.ic_chart_line_vertical_28: ImageVector
    get() {
        if (_ic_chart_line_vertical_28 != null) return _ic_chart_line_vertical_28!!
        _ic_chart_line_vertical_28 = ImageVector.Builder(
            name = "ic_chart_line_vertical_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.47363 17.4873C6.16399 17.4873 6.72363 18.0469 6.72363 18.7373V22.5264C6.72337 23.2165 6.16383 23.7764 5.47363 23.7764C4.78346 23.7763 4.2239 23.2165 4.22363 22.5264V18.7373C4.22363 18.047 4.7833 17.4873 5.47363 17.4873ZM11.1582 13.6973C11.8483 13.6975 12.4081 14.2571 12.4082 14.9473V22.5264C12.4081 23.2165 11.8483 23.7761 11.1582 23.7764C10.4679 23.7763 9.90829 23.2166 9.9082 22.5264V14.9473C9.90827 14.257 10.4679 13.6973 11.1582 13.6973ZM16.8428 8.96094C17.5329 8.9612 18.0928 9.52074 18.0928 10.2109V22.5264C18.0925 23.2163 17.5327 23.7761 16.8428 23.7764C16.1526 23.7763 15.593 23.2165 15.5928 22.5264V10.2109C15.5928 9.5206 16.1524 8.96097 16.8428 8.96094ZM22.5264 4.22363C23.2167 4.22363 23.7764 4.78328 23.7764 5.47363V22.5264C23.7763 23.2167 23.2167 23.7764 22.5264 23.7764C21.8361 23.7763 21.2764 23.2167 21.2764 22.5264V5.47363C21.2764 4.78329 21.836 4.22366 22.5264 4.22363Z"),
            )
        }.build()
        return _ic_chart_line_vertical_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartLineVertical28Preview() {
    Icon(
        imageVector = Icons.ic_chart_line_vertical_28,
        contentDescription = null,
    )
}