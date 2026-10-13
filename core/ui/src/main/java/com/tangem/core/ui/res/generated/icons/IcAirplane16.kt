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

private var _ic_airplane_16: ImageVector? = null

val Icons.ic_airplane_16: ImageVector
    get() {
        if (_ic_airplane_16 != null) return _ic_airplane_16!!
        _ic_airplane_16 = ImageVector.Builder(
            name = "ic_airplane_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.2354 3.47741C10.8789 2.84119 11.8712 2.84119 12.5147 3.47741C13.1618 4.11748 13.1619 5.10983 12.5147 5.74987L11.1046 7.14343L11.9376 11.2118H11.9356C12.0234 11.5654 11.9195 11.9938 11.5841 12.2557L11.2647 12.5721C10.9705 12.863 10.5753 12.9355 10.2481 12.871C9.93502 12.8091 9.58896 12.5979 9.44442 12.2069L9.44344 12.2079L8.48934 9.77819L7.67391 10.5848L7.74715 11.3788C7.74888 11.3976 7.75007 11.4165 7.75008 11.4354C7.75008 11.7098 7.65914 12.033 7.41414 12.2753L6.86434 12.8192C6.72835 12.9537 6.53851 13.0184 6.34872 12.995C6.15887 12.9716 5.98995 12.8626 5.89071 12.6991L4.92196 11.1034L3.30575 10.1444C3.15154 10.0528 3.04418 9.89874 3.01082 9.72253C2.97756 9.54633 3.02181 9.36406 3.13192 9.22253L3.63192 8.57995L3.68563 8.5194C3.92971 8.27809 4.25341 8.1903 4.52547 8.1903L4.58114 8.19323L5.39071 8.2655L6.19247 7.47351L3.70418 6.53308C3.68526 6.52592 3.66666 6.51761 3.64852 6.50866C3.02565 6.20075 2.73322 5.30875 3.33602 4.71276L3.68563 4.36706L3.80282 4.26843C3.92388 4.1822 4.05127 4.13229 4.1495 4.10241C4.28932 4.05987 4.43823 4.03796 4.57528 4.03796L4.69735 4.04968L8.82137 4.87487L10.2354 3.47741ZM11.6358 4.36608C11.4793 4.21134 11.2709 4.21134 11.1143 4.36608L9.46493 5.99694C9.31722 6.14298 9.1061 6.20661 8.90243 6.16589L4.53622 5.29284C4.53181 5.2937 4.52744 5.29383 4.52352 5.29479L4.36922 5.44811L7.54598 6.64929C7.74507 6.72455 7.89242 6.89617 7.93661 7.10436C7.9806 7.31242 7.91597 7.52906 7.76473 7.67858L6.06454 9.35925C5.93375 9.48846 5.75254 9.55336 5.56942 9.53698L4.58407 9.44811L5.69442 10.1073L5.75692 10.1503C5.81748 10.197 5.86931 10.2544 5.90926 10.3202L6.48153 11.2635L6.40243 10.4051C6.38538 10.219 6.45312 10.0346 6.58602 9.90319L8.28524 8.22253C8.4347 8.07476 8.64895 8.01203 8.85457 8.05554C9.0602 8.09905 9.22993 8.24368 9.30672 8.43933L10.5255 11.5448L10.6954 11.3768L9.81258 7.06237C9.77029 6.85562 9.8355 6.64151 9.98543 6.49304L11.6358 4.8612C11.7883 4.7104 11.7883 4.51689 11.6358 4.36608Z"),
            )
        }.build()
        return _ic_airplane_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAirplane16Preview() {
    Icon(
        imageVector = Icons.ic_airplane_16,
        contentDescription = null,
    )
}