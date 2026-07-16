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

private var _ic_arrow_download_20: ImageVector? = null

val Icons.ic_arrow_download_20: ImageVector
    get() {
        if (_ic_arrow_download_20 != null) return _ic_arrow_download_20!!
        _ic_arrow_download_20 = ImageVector.Builder(
            name = "ic_arrow_download_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.7549 15.502C17.1688 15.5023 17.5049 15.838 17.5049 16.252C17.5047 16.6657 17.1686 17.0016 16.7549 17.002H3.25C2.83593 17.002 2.50023 16.666 2.5 16.252C2.5 15.8377 2.83579 15.502 3.25 15.502H16.7549Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.002 3C10.4161 3 10.7518 3.33589 10.752 3.75V11.6631L13.4717 8.94336C13.7646 8.65082 14.2395 8.65059 14.5322 8.94336C14.8249 9.23615 14.8248 9.71102 14.5322 10.0039L10.5322 14.0039C10.4383 14.0978 10.3205 14.1605 10.1943 14.1943C10.1793 14.1983 10.1648 14.2049 10.1494 14.208C10.1468 14.2085 10.1442 14.2085 10.1416 14.209C10.1025 14.2164 10.0625 14.2197 10.0215 14.2207C10.015 14.2209 10.0085 14.2236 10.002 14.2236C9.99521 14.2236 9.98817 14.2209 9.98145 14.2207C9.94114 14.2196 9.90176 14.2162 9.86328 14.209C9.85944 14.2083 9.85538 14.2088 9.85156 14.208C9.84522 14.2067 9.83929 14.2036 9.83301 14.2021C9.69737 14.1707 9.57171 14.1038 9.47168 14.0039L5.47168 10.0039C5.17881 9.71101 5.1788 9.23624 5.47168 8.94336C5.76461 8.65082 6.23946 8.65059 6.53223 8.94336L9.25195 11.6631V3.75C9.25208 3.33594 9.58788 3.00007 10.002 3Z"),
            )
        }.build()
        return _ic_arrow_download_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownload20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_download_20,
        contentDescription = null,
    )
}