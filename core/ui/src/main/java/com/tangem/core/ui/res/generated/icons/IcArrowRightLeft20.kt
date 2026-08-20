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

private var _ic_arrow_right_left_20: ImageVector? = null

val Icons.ic_arrow_right_left_20: ImageVector
    get() {
        if (_ic_arrow_right_left_20 != null) return _ic_arrow_right_left_20!!
        _ic_arrow_right_left_20 = ImageVector.Builder(
            name = "ic_arrow_right_left_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.2207 9.71209C6.51368 9.41969 6.98856 9.41929 7.28125 9.71209C7.57374 10.0048 7.57351 10.4798 7.28125 10.7726L5.56152 12.4914H16.251C16.6649 12.4916 17.0008 12.8275 17.001 13.2414C17.001 13.6554 16.665 13.9911 16.251 13.9914H5.56152L7.28125 15.7101C7.57382 16.0028 7.5734 16.4778 7.28125 16.7707C6.98839 17.0635 6.5136 17.0635 6.2207 16.7707L3.2207 13.7717C3.1104 13.6613 3.03882 13.5201 3.0127 13.3683C3.00662 13.3326 3.00279 13.2962 3.00195 13.259C3.00182 13.2532 3.00098 13.2472 3.00098 13.2414C3.00098 13.2352 3.0018 13.229 3.00195 13.2228C3.00286 13.1863 3.00674 13.1505 3.0127 13.1154C3.0387 12.9632 3.11018 12.8206 3.2207 12.7101L6.2207 9.71209Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7188 3.22478C13.0117 2.93204 13.4865 2.93194 13.7793 3.22478L16.7793 6.22283C16.9149 6.35848 16.9989 6.54616 16.999 6.7531C16.999 6.80079 16.9929 6.84753 16.9844 6.89275C16.9566 7.04018 16.8867 7.1779 16.7793 7.28533L13.7793 10.2834C13.4865 10.5761 13.0116 10.576 12.7188 10.2834C12.426 9.9905 12.4261 9.51567 12.7188 9.22283L14.4395 7.5031H3.75C3.33579 7.5031 2.99902 7.16731 2.99902 6.7531C2.99919 6.33903 3.33589 6.0031 3.75 6.0031H14.4375L12.7188 4.28533C12.426 3.99254 12.4262 3.5177 12.7188 3.22478Z"),
            )
        }.build()
        return _ic_arrow_right_left_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRightLeft20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_left_20,
        contentDescription = null,
    )
}