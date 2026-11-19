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

private var _ic_arrow_cross_horizontal_20: ImageVector? = null

val Icons.ic_arrow_cross_horizontal_20: ImageVector
    get() {
        if (_ic_arrow_cross_horizontal_20 != null) return _ic_arrow_cross_horizontal_20!!
        _ic_arrow_cross_horizontal_20 = ImageVector.Builder(
            name = "ic_arrow_cross_horizontal_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.84961 11.2754C10.181 11.0269 10.6519 11.0934 10.9004 11.4248L11.5752 12.3252C11.8994 12.7574 12.3613 12.9999 12.8496 13H14.9395L14.7197 12.7803C14.4268 12.4874 14.4268 12.0126 14.7197 11.7197C15.0126 11.4268 15.4874 11.4268 15.7803 11.7197L17.2803 13.2197C17.4209 13.3604 17.5 13.5511 17.5 13.75C17.5 13.9489 17.4209 14.1396 17.2803 14.2803L15.7803 15.7803C15.4874 16.0732 15.0126 16.0732 14.7197 15.7803C14.4268 15.4874 14.4268 15.0126 14.7197 14.7197L14.9395 14.5H12.8496C11.8383 14.4999 10.9506 13.9921 10.375 13.2246L9.7002 12.3252C9.45178 11.994 9.51862 11.524 9.84961 11.2754ZM14.7197 4.21973C15.0126 3.92684 15.4874 3.92684 15.7803 4.21973L17.2803 5.71973C17.4209 5.86038 17.5 6.05109 17.5 6.25C17.5 6.44892 17.4209 6.63963 17.2803 6.78028L15.7803 8.28028C15.4874 8.57317 15.0126 8.57317 14.7197 8.28028C14.4268 7.98739 14.4268 7.51262 14.7197 7.21973L14.9395 7H12.8496C12.3613 7.00013 11.8994 7.24264 11.5752 7.67481L11.5732 7.67774L7.37305 13.2275C6.79739 13.9936 5.91056 14.4999 4.90039 14.5H3.25C2.83579 14.5 2.50001 14.1642 2.5 13.75C2.5 13.3358 2.83579 13 3.25 13H4.90039C5.3887 12.9999 5.85058 12.7574 6.1748 12.3252L6.17676 12.3223L10.377 6.77246C10.9526 6.00642 11.8394 5.50013 12.8496 5.5H14.9395L14.7197 5.28028C14.4268 4.98738 14.4268 4.51262 14.7197 4.21973ZM4.90039 5.5C5.91169 5.50013 6.79939 6.00792 7.375 6.77539L8.0498 7.67481C8.29822 8.00603 8.23139 8.476 7.90039 8.72461C7.56902 8.97314 7.09814 8.90657 6.84961 8.5752L6.1748 7.67481C5.85058 7.24264 5.38871 7.00013 4.90039 7H3.25C2.83579 7 2.5 6.66422 2.5 6.25C2.5 5.83579 2.83579 5.5 3.25 5.5H4.90039Z"),
            )
        }.build()
        return _ic_arrow_cross_horizontal_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCrossHorizontal20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_cross_horizontal_20,
        contentDescription = null,
    )
}