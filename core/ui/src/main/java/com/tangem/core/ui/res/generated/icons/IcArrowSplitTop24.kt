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

private var _ic_arrow_split_top_24: ImageVector? = null

val Icons.ic_arrow_split_top_24: ImageVector
    get() {
        if (_ic_arrow_split_top_24 != null) return _ic_arrow_split_top_24!!
        _ic_arrow_split_top_24 = ImageVector.Builder(
            name = "ic_arrow_split_top_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.77041 8.92145C4.24409 9.59579 3.54897 9.34787 3.30071 8.48512L2.04947 4.19119C1.85087 3.50693 2.26794 2.97143 2.98293 3.00118L7.46155 3.14993C8.35528 3.17968 8.7525 3.80444 8.23612 4.46885L7.34238 5.60928L7.3821 5.62911C9.2093 6.83895 11.4436 9.35779 11.9799 11.0238H12.0196C12.5459 9.36771 14.7902 6.83895 16.6174 5.62911L16.667 5.59936L15.7534 4.33994C15.2569 3.65568 15.6641 3.04085 16.5677 3.05077L21.0463 3.03093C21.7613 3.03093 22.1585 3.58627 21.9401 4.27052L20.5498 8.51487C20.2817 9.36771 19.5667 9.59579 19.0603 8.8917L18.097 7.56287L17.8785 7.7017C15.8825 8.97104 13.2013 12.0948 13.2013 14.3657V19.6712C13.2013 20.5339 12.7743 21 11.9997 21C11.2252 21 10.8081 20.5339 10.8081 19.6712V14.3657C10.8081 12.0948 8.10702 8.97104 6.12094 7.7017L5.85282 7.53312L4.77041 8.92145Z"),
            )
        }.build()
        return _ic_arrow_split_top_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowSplitTop24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_split_top_24,
        contentDescription = null,
    )
}