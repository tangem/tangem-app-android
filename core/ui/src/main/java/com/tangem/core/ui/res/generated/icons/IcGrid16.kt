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

private var _ic_grid_16: ImageVector? = null

val Icons.ic_grid_16: ImageVector
    get() {
        if (_ic_grid_16 != null) return _ic_grid_16!!
        _ic_grid_16 = ImageVector.Builder(
            name = "ic_grid_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.46191 8.71777C6.46661 8.71817 7.28125 9.53232 7.28125 10.5371V12.1797C7.28096 13.1842 6.46643 13.9986 5.46191 13.999H3.81934C2.81448 13.999 2.0003 13.1845 2 12.1797V10.5371C2 9.53207 2.8143 8.71777 3.81934 8.71777H5.46191ZM3.81934 9.96777C3.50465 9.96777 3.25 10.2224 3.25 10.5371V12.1797C3.25029 12.4941 3.50484 12.749 3.81934 12.749H5.46191C5.77607 12.7486 6.03096 12.4939 6.03125 12.1797V10.5371C6.03125 10.2227 5.77626 9.96817 5.46191 9.96777H3.81934Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1797 8.71777C13.1844 8.71818 13.999 9.53233 13.999 10.5371V12.1797C13.9987 13.1842 13.1842 13.9986 12.1797 13.999H10.5371C9.53225 13.999 8.71807 13.1845 8.71777 12.1797V10.5371C8.71777 9.53207 9.53207 8.71777 10.5371 8.71777H12.1797ZM10.5371 9.96777C10.2224 9.96777 9.96777 10.2224 9.96777 10.5371V12.1797C9.96807 12.4941 10.2226 12.749 10.5371 12.749H12.1797C12.4938 12.7486 12.7487 12.4939 12.749 12.1797V10.5371C12.749 10.2227 12.494 9.96818 12.1797 9.96777H10.5371Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.46191 2C6.46661 2.0004 7.28125 2.81454 7.28125 3.81934V5.46191C7.2808 6.46632 6.46634 7.28085 5.46191 7.28125H3.81934C2.81458 7.28125 2.00045 6.46657 2 5.46191V3.81934C2 2.8143 2.8143 2 3.81934 2H5.46191ZM3.81934 3.25C3.50465 3.25 3.25 3.50465 3.25 3.81934V5.46191C3.25045 5.77621 3.50493 6.03125 3.81934 6.03125H5.46191C5.77598 6.03085 6.0308 5.77597 6.03125 5.46191V3.81934C6.03125 3.5049 5.77626 3.2504 5.46191 3.25H3.81934Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1797 2C13.1844 2.00041 13.999 2.81455 13.999 3.81934V5.46191C13.9986 6.46632 13.1841 7.28084 12.1797 7.28125H10.5371C9.53235 7.28125 8.71822 6.46657 8.71777 5.46191V3.81934C8.71777 2.8143 9.53207 2 10.5371 2H12.1797ZM10.5371 3.25C10.2224 3.25 9.96777 3.50465 9.96777 3.81934V5.46191C9.96822 5.77621 10.2227 6.03125 10.5371 6.03125H12.1797C12.4937 6.03084 12.7486 5.77596 12.749 5.46191V3.81934C12.749 3.50491 12.494 3.25041 12.1797 3.25H10.5371Z"),
            )
        }.build()
        return _ic_grid_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGrid16Preview() {
    Icon(
        imageVector = Icons.ic_grid_16,
        contentDescription = null,
    )
}