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

private var _ic_bell_cross_20: ImageVector? = null

val Icons.ic_bell_cross_20: ImageVector
    get() {
        if (_ic_bell_cross_20 != null) return _ic_bell_cross_20!!
        _ic_bell_cross_20 = ImageVector.Builder(
            name = "ic_bell_cross_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.07252 3.07764C3.3653 2.7851 3.8402 2.78508 4.13306 3.07764L16.928 15.8608C17.2208 16.1535 17.2204 16.6284 16.928 16.9214C16.6353 17.2144 16.1605 17.215 15.8674 16.9224L14.5276 15.5835H12.7385C12.6635 16.1894 12.3897 16.7577 11.9534 17.1938C11.4365 17.71 10.7346 17.9995 10.0042 17.9995C9.27332 17.9999 8.57114 17.7112 8.05396 17.1948C7.6172 16.7586 7.34477 16.1898 7.26978 15.5835H5.21021C4.57276 15.5835 3.96104 15.3298 3.51002 14.8794C3.05901 14.4287 2.80504 13.8168 2.80494 13.1792L2.81763 12.9321C2.87484 12.3604 3.12781 11.8228 3.53736 11.4126L4.41724 10.5317V5.48096L3.07252 4.13818C2.77978 3.8454 2.77985 3.37056 3.07252 3.07764ZM8.7942 15.5835C8.85191 15.7893 8.96061 15.9796 9.11451 16.1333C9.35005 16.3683 9.67009 16.5006 10.0032 16.5005C10.3363 16.5005 10.6573 16.3674 10.8928 16.1323C11.0464 15.9787 11.1564 15.7888 11.2141 15.5835H8.7942ZM5.91724 10.8442C5.91712 11.0428 5.83784 11.234 5.69752 11.3745L4.59791 12.4731L4.59693 12.4722C4.40976 12.66 4.30524 12.9153 4.30494 13.1802C4.3052 13.4194 4.40015 13.6495 4.56959 13.8188C4.73924 13.9882 4.97019 14.0835 5.21021 14.0835H13.0266L5.91724 6.97998V10.8442Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0032 2.00049C13.0883 2.00049 15.5901 4.50026 15.5901 7.58447V10.5679C15.5896 10.9817 15.254 11.3179 14.8401 11.3179C14.4263 11.3177 14.0906 10.9816 14.0901 10.5679V7.58447C14.0901 5.32968 12.2608 3.50049 10.0032 3.50049C9.23136 3.50055 8.51118 3.71344 7.89674 4.0835C7.54213 4.29673 7.08118 4.18291 6.86744 3.82861C6.65381 3.47391 6.76877 3.01214 7.1233 2.79834C7.96477 2.29163 8.95129 2.00055 10.0032 2.00049Z"),
            )
        }.build()
        return _ic_bell_cross_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBellCross20Preview() {
    Icon(
        imageVector = Icons.ic_bell_cross_20,
        contentDescription = null,
    )
}