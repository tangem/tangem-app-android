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

private var _ic_rose_20: ImageVector? = null

val Icons.ic_rose_20: ImageVector
    get() {
        if (_ic_rose_20 != null) return _ic_rose_20!!
        _ic_rose_20 = ImageVector.Builder(
            name = "ic_rose_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.75 2.5C12.1754 2.50002 13.3663 3.49447 13.6719 4.82715C15.0051 5.13234 16 6.32418 16 7.75C16 8.85979 15.3958 9.82579 14.5 10.3447V10.75C14.5 12.9792 12.8778 14.8292 10.75 15.1865V16.75C10.75 17.1641 10.4141 17.4999 10 17.5C9.58579 17.5 9.25 17.1642 9.25 16.75V15.1865C7.12194 14.8294 5.50004 12.9794 5.5 10.75V9.80664C4.584 9.05001 4 7.90571 4 6.625C4 4.34704 5.84705 2.5 8.125 2.5H10.75ZM7 10.75C7.00005 12.4065 8.34351 13.75 10 13.75C11.6046 13.7499 12.9157 12.489 12.9961 10.9043L13 10.75H8.125C7.73482 10.75 7.35778 10.694 7 10.5928V10.75ZM8.125 4C6.67548 4 5.5 5.17546 5.5 6.625C5.5 8.07454 6.67548 9.25 8.125 9.25H13C13.8285 9.24997 14.5 8.57852 14.5 7.75C14.5 7.1417 14.1375 6.61919 13.6172 6.38379C13.24 7.60925 12.0993 8.49998 10.75 8.5H8.55859C7.52338 8.5 6.68359 7.66021 6.68359 6.625C6.68359 5.58979 7.52338 4.75 8.55859 4.75H10.0811C10.4952 4.75005 10.8311 5.08582 10.8311 5.5C10.8311 5.91418 10.4952 6.24995 10.0811 6.25H8.55859C8.35182 6.25 8.18359 6.41821 8.18359 6.625C8.18359 6.83179 8.35182 7 8.55859 7H10.75C11.5267 6.99998 12.1654 6.4098 12.2422 5.65332L12.25 5.5L12.2422 5.34668C12.1654 4.5902 11.5267 4.00002 10.75 4H8.125Z"),
            )
        }.build()
        return _ic_rose_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRose20Preview() {
    Icon(
        imageVector = Icons.ic_rose_20,
        contentDescription = null,
    )
}