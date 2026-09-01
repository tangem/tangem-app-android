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

private var _ic_user_plus_28: ImageVector? = null

val Icons.ic_user_plus_28: ImageVector
    get() {
        if (_ic_user_plus_28 != null) return _ic_user_plus_28!!
        _ic_user_plus_28 = ImageVector.Builder(
            name = "ic_user_plus_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.8154 15.9814C15.5057 15.9814 16.0653 16.5412 16.0654 17.2314C16.0654 17.9218 15.5058 18.4814 14.8154 18.4814H9.32715C7.4897 18.4814 6.00014 19.9712 6 21.8086V22.9531C5.99987 23.6434 5.44028 24.2031 4.75 24.2031C4.05972 24.2031 3.50013 23.6434 3.5 22.9531V21.8086C3.50014 18.5905 6.10899 15.9814 9.32715 15.9814H14.8154Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.8174 14.8369C21.5077 14.8369 22.0673 15.3967 22.0674 16.0869V18.2705H24.251C24.9409 18.271 25.501 18.8305 25.501 19.5205C25.5007 20.2103 24.9407 20.77 24.251 20.7705H22.0674V22.9531C22.0671 23.6433 21.5076 24.2031 20.8174 24.2031C20.1272 24.2031 19.5676 23.6433 19.5674 22.9531V20.7705H17.3848C16.6946 20.7705 16.135 20.2106 16.1348 19.5205C16.1348 18.8302 16.6944 18.2705 17.3848 18.2705H19.5674V16.0869C19.5675 15.3967 20.1271 14.8369 20.8174 14.8369Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.9062 2.25C17.1242 2.25026 19.7333 4.85918 19.7334 8.07715C19.7334 11.2952 17.1243 13.904 13.9062 13.9043C10.688 13.9043 8.0791 11.2954 8.0791 8.07715C8.07924 4.85902 10.6881 2.25 13.9062 2.25ZM13.9062 4.75C12.0688 4.75 10.5792 6.23973 10.5791 8.07715C10.5791 9.91468 12.0687 11.4043 13.9062 11.4043C15.7436 11.404 17.2334 9.91452 17.2334 8.07715C17.2333 6.23989 15.7435 4.75026 13.9062 4.75Z"),
            )
        }.build()
        return _ic_user_plus_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus28Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_28,
        contentDescription = null,
    )
}