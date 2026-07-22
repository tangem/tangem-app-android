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

private var _ic_globe_16: ImageVector? = null

val Icons.ic_globe_16: ImageVector
    get() {
        if (_ic_globe_16 != null) return _ic_globe_16!!
        _ic_globe_16 = ImageVector.Builder(
            name = "ic_globe_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 1.9375C11.3548 1.9377 14.0625 4.6451 14.0625 8C14.0623 11.3547 11.3547 14.0623 8 14.0625C4.64513 14.0625 1.93774 11.3548 1.9375 8C1.9375 4.64498 4.64498 1.9375 8 1.9375ZM3.22852 8.625C3.47332 10.5242 4.81767 12.0701 6.60645 12.6074C5.82612 11.3987 5.38632 10.023 5.28516 8.625H3.22852ZM10.7148 8.625C10.6137 10.0231 10.173 11.3986 9.39258 12.6074C11.1816 12.0702 12.5257 10.5244 12.7705 8.625H10.7148ZM6.53711 8.625C6.65176 9.97856 7.13846 11.3044 7.99902 12.417C8.85993 11.3042 9.34723 9.97886 9.46191 8.625H6.53711ZM6.60645 3.3916C4.81739 3.92897 3.4731 5.47542 3.22852 7.375H5.28516C5.38623 5.97668 5.82587 4.60059 6.60645 3.3916ZM7.99902 3.58203C7.13819 4.69489 6.65167 6.02109 6.53711 7.375H9.46191C9.34733 6.02087 8.86013 4.695 7.99902 3.58203ZM9.39258 3.3916C10.1732 4.60062 10.6138 5.97663 10.7148 7.375H12.7705C12.5259 5.47535 11.1818 3.92888 9.39258 3.3916Z"),
            )
        }.build()
        return _ic_globe_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGlobe16Preview() {
    Icon(
        imageVector = Icons.ic_globe_16,
        contentDescription = null,
    )
}