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

private var _ic_filter_32: ImageVector? = null

val Icons.ic_filter_32: ImageVector
    get() {
        if (_ic_filter_32 != null) return _ic_filter_32!!
        _ic_filter_32 = ImageVector.Builder(
            name = "ic_filter_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.2461 17.0059C15.0344 17.0059 17.3678 18.9209 18.0303 21.5029H27.5C28.3284 21.5029 29 22.1745 29 23.0029C28.9999 23.8313 28.3284 24.5029 27.5 24.5029H18.0303C17.3677 27.0848 15.0343 29 12.2461 29C9.45808 28.9998 7.12545 27.0847 6.46289 24.5029H4.5C3.67161 24.5029 3.00007 23.8313 3 23.0029C3 22.1745 3.67157 21.5029 4.5 21.5029H6.46289C7.12537 18.9211 9.458 17.0061 12.2461 17.0059ZM12.2461 20.0059C10.6126 20.0061 9.27441 21.3399 9.27441 23.0029C9.27448 24.6659 10.6127 25.9998 12.2461 26C13.8797 26 15.2187 24.6661 15.2188 23.0029C15.2188 21.3397 13.8797 20.0059 12.2461 20.0059ZM19.4336 3C22.2218 3 24.5552 4.91516 25.2178 7.49707H27.5C28.3284 7.49708 28.9999 8.16874 29 8.99707C29 9.82549 28.3284 10.4971 27.5 10.4971H25.2178C24.5553 13.0791 22.2219 14.9941 19.4336 14.9941C16.6455 14.9939 14.3129 13.0789 13.6504 10.4971H4.5C3.67158 10.4971 3 9.82549 3 8.99707C3.0001 8.16873 3.67164 7.49708 4.5 7.49707H13.6504C14.3129 4.91531 16.6456 3.0002 19.4336 3ZM19.4336 6C17.8002 6.00024 16.462 7.33409 16.4619 8.99707C16.4619 10.6601 17.8001 11.9939 19.4336 11.9941C21.0672 11.9941 22.4062 10.6603 22.4062 8.99707C22.4062 7.33394 21.0672 6 19.4336 6Z"),
            )
        }.build()
        return _ic_filter_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFilter32Preview() {
    Icon(
        imageVector = Icons.ic_filter_32,
        contentDescription = null,
    )
}