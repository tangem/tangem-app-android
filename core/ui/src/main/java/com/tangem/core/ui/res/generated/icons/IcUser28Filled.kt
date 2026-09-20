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

private var _ic_user_28_filled: ImageVector? = null

val Icons.ic_user_28_filled: ImageVector
    get() {
        if (_ic_user_28_filled != null) return _ic_user_28_filled!!
        _ic_user_28_filled = ImageVector.Builder(
            name = "ic_user_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.34229 4.5297C11.8817 2.13973 15.7381 2.06471 18.3599 4.30509L18.6099 4.5297L18.6362 4.55607C21.2492 7.16897 21.2897 11.3816 18.6099 13.9037C16.0403 16.3218 11.843 16.5625 9.28955 13.85C6.87142 11.2806 6.62988 7.08308 9.34229 4.5297Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.2104 17.2817C21.0663 17.2818 23.9995 20.3778 23.9995 24.0707C23.9993 24.7608 23.4396 25.3206 22.7495 25.3207H5.25049C4.56031 25.3206 4.00068 24.7609 4.00049 24.0707C4.00049 20.2148 7.09659 17.2818 10.7896 17.2817H17.2104Z"),
            )
        }.build()
        return _ic_user_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser28FilledPreview() {
    Icon(
        imageVector = Icons.ic_user_28_filled,
        contentDescription = null,
    )
}