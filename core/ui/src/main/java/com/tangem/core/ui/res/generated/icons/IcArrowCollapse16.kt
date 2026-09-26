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

private var _ic_arrow_collapse_16: ImageVector? = null

val Icons.ic_arrow_collapse_16: ImageVector
    get() {
        if (_ic_arrow_collapse_16 != null) return _ic_arrow_collapse_16!!
        _ic_arrow_collapse_16 = ImageVector.Builder(
            name = "ic_arrow_collapse_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.03315 8.26367C7.42119 8.26367 7.73612 8.5788 7.73628 8.9668V11.8516C7.73628 12.1967 7.45636 12.4764 7.11128 12.4766C6.76619 12.4765 6.48628 12.1967 6.48628 11.8516V10.3975L4.44233 12.4414C4.19826 12.6855 3.80165 12.6855 3.55757 12.4414C3.31415 12.1973 3.31381 11.8015 3.55757 11.5576L5.60249 9.51367H4.14839C3.8033 9.51356 3.52339 9.23378 3.52339 8.88867C3.52355 8.54369 3.8034 8.26378 4.14839 8.26367H7.03315Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.5576 3.55859C11.8016 3.31452 12.1983 3.31455 12.4423 3.55859C12.686 3.80266 12.6862 4.19842 12.4423 4.44238L10.3974 6.48633H11.8515C12.1967 6.48633 12.4765 6.76615 12.4765 7.11133C12.4764 7.45644 12.1966 7.73633 11.8515 7.73633H8.96675C8.57875 7.73622 8.2637 7.4212 8.26362 7.0332V4.14844C8.26362 3.8034 8.54364 3.52366 8.88862 3.52344C9.2338 3.52344 9.51362 3.80326 9.51362 4.14844V5.60254L11.5576 3.55859Z"),
            )
        }.build()
        return _ic_arrow_collapse_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCollapse16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_collapse_16,
        contentDescription = null,
    )
}