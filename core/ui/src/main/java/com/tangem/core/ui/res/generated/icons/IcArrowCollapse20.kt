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

private var _ic_arrow_collapse_20: ImageVector? = null

val Icons.ic_arrow_collapse_20: ImageVector
    get() {
        if (_ic_arrow_collapse_20 != null) return _ic_arrow_collapse_20!!
        _ic_arrow_collapse_20 = ImageVector.Builder(
            name = "ic_arrow_collapse_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.79193 10.3614C9.0257 10.3615 9.23733 10.4562 9.39056 10.6094C9.40979 10.6287 9.42757 10.6493 9.44427 10.67C9.56524 10.8164 9.63861 11.0042 9.63861 11.209V14.8155C9.63825 15.2294 9.3026 15.5655 8.88861 15.5655C8.47473 15.5653 8.13897 15.2293 8.13861 14.8155V12.9219L5.53021 15.5303C5.23735 15.823 4.76252 15.823 4.46966 15.5303C4.17693 15.2375 4.17695 14.7626 4.46966 14.4698L7.07806 11.8614H5.18548C4.77137 11.8614 4.43563 11.5255 4.43548 11.1114C4.43548 10.6972 4.77128 10.3614 5.18548 10.3614H8.79193Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.4697 4.46977C14.7625 4.17698 15.2373 4.177 15.5302 4.46977C15.823 4.76265 15.823 5.23745 15.5302 5.53032L12.9218 8.13872H14.8144C15.2285 8.13872 15.5643 8.47458 15.5644 8.88872C15.5644 9.30293 15.2286 9.63872 14.8144 9.63872H11.2079C10.9742 9.63862 10.7625 9.54385 10.6093 9.39067C10.5901 9.37142 10.5723 9.35084 10.5556 9.33012C10.4346 9.18363 10.3613 8.99589 10.3613 8.79106V5.18461C10.3616 4.77067 10.6973 4.43463 11.1113 4.43461C11.5252 4.43473 11.861 4.77073 11.8613 5.18461V7.07817L14.4697 4.46977Z"),
            )
        }.build()
        return _ic_arrow_collapse_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCollapse20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_collapse_20,
        contentDescription = null,
    )
}