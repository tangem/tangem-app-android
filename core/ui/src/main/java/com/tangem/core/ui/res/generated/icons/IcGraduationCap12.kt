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

private var _ic_graduation_cap_12: ImageVector? = null

val Icons.ic_graduation_cap_12: ImageVector
    get() {
        if (_ic_graduation_cap_12 != null) return _ic_graduation_cap_12!!
        _ic_graduation_cap_12 = ImageVector.Builder(
            name = "ic_graduation_cap_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.7901 2.30466C5.54331 1.89847 6.45682 1.89842 7.21002 2.30466L9.82525 3.71774C10.7245 4.20364 10.7246 5.47308 9.82525 5.95895L9.044 6.38083V7.69821C9.04385 8.26163 8.75385 8.78263 8.28033 9.08884L7.59967 9.52927C6.62968 10.1566 5.36876 10.1565 4.39849 9.52927L3.71783 9.08884C3.24519 8.7829 2.95431 8.26251 2.95416 7.69821V6.37985L2.17486 5.95895C1.27535 5.47312 1.27539 4.20361 2.17486 3.71774L4.7901 2.30466ZM3.95416 7.69821C3.95431 7.91473 4.06599 8.12267 4.2608 8.24899L4.94146 8.68942L5.06353 8.76169C5.68488 9.10116 6.45623 9.07697 7.05572 8.68942L7.73639 8.24899C7.93125 8.12294 8.04385 7.91482 8.044 7.69821V6.92087L7.211 7.37204C6.45792 7.77818 5.54295 7.77823 4.7901 7.37204L3.95416 6.91989V7.69821ZM6.73541 3.18454C6.3356 2.96891 5.85937 2.94274 5.44049 3.10446L5.26471 3.18454L2.65045 4.59763C2.45132 4.70566 2.45031 4.97107 2.64947 5.07907L5.26471 6.49216C5.72115 6.73846 6.27841 6.73774 6.73541 6.49118L9.34967 5.07907C9.54864 4.97104 9.54958 4.70568 9.35064 4.59763L6.73541 3.18454Z"),
            )
        }.build()
        return _ic_graduation_cap_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGraduationCap12Preview() {
    Icon(
        imageVector = Icons.ic_graduation_cap_12,
        contentDescription = null,
    )
}