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

private var _ic_percent_16: ImageVector? = null

val Icons.ic_percent_16: ImageVector
    get() {
        if (_ic_percent_16 != null) return _ic_percent_16!!
        _ic_percent_16 = ImageVector.Builder(
            name = "ic_percent_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M9.87392 9.87313C10.8176 8.92951 12.3472 8.92957 13.2909 9.87313L13.4569 10.0567C14.231 11.0058 14.1756 12.4055 13.2909 13.2901C12.3472 14.2335 10.8176 14.2337 9.87392 13.2901C8.93031 12.3465 8.93038 10.8168 9.87392 9.87313ZM12.3192 10.6768C11.861 10.3029 11.1849 10.3298 10.7577 10.7569C10.3023 11.2124 10.3023 11.9509 10.7577 12.4063C11.2132 12.8617 11.9516 12.8616 12.4071 12.4063C12.8342 11.9793 12.8608 11.3039 12.4872 10.8458L12.4071 10.7569L12.3192 10.6768Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.036 3.07723C12.2799 2.83356 12.6757 2.83397 12.9198 3.07723C13.1639 3.32131 13.1639 3.71793 12.9198 3.962L3.96279 12.919C3.7187 13.1631 3.32209 13.1631 3.07802 12.919C2.83453 12.6751 2.83454 12.2792 3.07802 12.0352L12.036 3.07723Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M2.70791 2.70712C3.65162 1.76369 5.18124 1.76359 6.1249 2.70712L6.29091 2.89071C7.06491 3.83975 7.00942 5.23944 6.1249 6.12411C5.18119 7.06765 3.6516 7.06768 2.70791 6.12411C1.76422 5.18044 1.76421 3.65078 2.70791 2.70712ZM5.15322 3.51083C4.69509 3.13693 4.0189 3.16394 3.59169 3.59091C3.13617 4.0464 3.1362 4.7848 3.59169 5.24032C4.04723 5.69574 4.78555 5.69571 5.24111 5.24032C5.66794 4.81331 5.69469 4.13783 5.32119 3.67977L5.24111 3.59091L5.15322 3.51083Z"),
            )
        }.build()
        return _ic_percent_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPercent16Preview() {
    Icon(
        imageVector = Icons.ic_percent_16,
        contentDescription = null,
    )
}