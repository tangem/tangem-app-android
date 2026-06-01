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

private var _ic_bell_20: ImageVector? = null

val Icons.ic_bell_20: ImageVector
    get() {
        if (_ic_bell_20 != null) return _ic_bell_20!!
        _ic_bell_20 = ImageVector.Builder(
            name = "ic_bell_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2.93652C12.973 2.93675 15.4256 5.27508 15.4258 8.21094V10.9453L16.2607 11.752C16.7205 12.1978 16.9838 12.8084 16.9844 13.4492C16.9841 14.7413 15.9089 15.7479 14.6348 15.748H12.2832C12.2186 16.2767 12.0159 16.7844 11.6787 17.1904C11.2611 17.6931 10.661 18.01 9.99902 18.0098C9.33733 18.01 8.73779 17.6929 8.32031 17.1904C7.98308 16.7844 7.78058 16.2767 7.71582 15.748H5.36426C4.74898 15.7479 4.15428 15.511 3.71191 15.083C3.26883 14.6542 3.01476 14.0676 3.01465 13.4502C3.01525 12.8094 3.27849 12.1987 3.73828 11.7529L3.73926 11.752L4.57324 10.9453V8.21094C4.57346 5.27493 7.02678 2.93652 10 2.93652ZM9.23535 15.748C9.28339 15.9377 9.36736 16.1044 9.47363 16.2324C9.64032 16.433 9.83385 16.5106 9.99805 16.5107L9.99902 16.9961L10 16.5107C10.1644 16.5107 10.3576 16.4323 10.5244 16.2314C10.6307 16.1035 10.7156 15.9374 10.7637 15.748H9.23535ZM10 4.43652C7.80819 4.43652 6.07346 6.14961 6.07324 8.21094V11.2637C6.07324 11.4668 5.99072 11.6614 5.84473 11.8027L4.78125 12.8311C4.6295 12.9787 4.53991 13.1681 4.51953 13.3652L4.51465 13.4502C4.51476 13.6536 4.5982 13.8542 4.75488 14.0059C4.91243 14.1582 5.13125 14.2479 5.36426 14.248H14.6348C15.0969 14.2479 15.4395 13.9133 15.4805 13.5273L15.4844 13.4502C15.4841 13.223 15.3914 12.9999 15.2178 12.8311L14.1543 11.8027C14.0083 11.6615 13.9258 11.4668 13.9258 11.2637V8.21094C13.9256 6.14974 12.1916 4.43675 10 4.43652Z"),
            )
        }.build()
        return _ic_bell_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBell20Preview() {
    Icon(
        imageVector = Icons.ic_bell_20,
        contentDescription = null,
    )
}