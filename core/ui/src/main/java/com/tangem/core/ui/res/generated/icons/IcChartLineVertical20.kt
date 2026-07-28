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

private var _ic_chart_line_vertical_20: ImageVector? = null

val Icons.ic_chart_line_vertical_20: ImageVector
    get() {
        if (_ic_chart_line_vertical_20 != null) return _ic_chart_line_vertical_20!!
        _ic_chart_line_vertical_20 = ImageVector.Builder(
            name = "ic_chart_line_vertical_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.31543 12.4082C4.72964 12.4082 5.06543 12.744 5.06543 13.1582V15.6846C5.06527 16.0986 4.72954 16.4346 4.31543 16.4346C3.90148 16.4344 3.56559 16.0985 3.56543 15.6846V13.1582C3.56543 12.7441 3.90138 12.4084 4.31543 12.4082Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.10547 9.88184C8.51946 9.8821 8.85547 10.2178 8.85547 10.6318V15.6846C8.85528 16.0985 8.51934 16.4343 8.10547 16.4346C7.69154 16.4344 7.35566 16.0985 7.35547 15.6846V10.6318C7.35547 10.2177 7.69142 9.88203 8.10547 9.88184Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.8945 6.72363C12.3087 6.72363 12.6445 7.05948 12.6445 7.47363V15.6846C12.6443 16.0986 12.3086 16.4346 11.8945 16.4346C11.4806 16.4344 11.1448 16.0985 11.1445 15.6846V7.47363C11.1446 7.0596 11.4805 6.72383 11.8945 6.72363Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.6846 3.56543C16.0984 3.56569 16.4344 3.90155 16.4346 4.31543V15.6836C16.4346 16.0976 16.0986 16.4333 15.6846 16.4336C15.2705 16.4334 14.9346 16.0977 14.9346 15.6836V4.31543C14.9348 3.9015 15.2706 3.56562 15.6846 3.56543Z"),
            )
        }.build()
        return _ic_chart_line_vertical_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartLineVertical20Preview() {
    Icon(
        imageVector = Icons.ic_chart_line_vertical_20,
        contentDescription = null,
    )
}