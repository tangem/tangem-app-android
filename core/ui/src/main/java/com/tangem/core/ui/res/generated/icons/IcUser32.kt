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

private var _ic_user_32: ImageVector? = null

val Icons.ic_user_32: ImageVector
    get() {
        if (_ic_user_32 != null) return _ic_user_32!!
        _ic_user_32 = ImageVector.Builder(
            name = "ic_user_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.4854 19.4502C23.7614 19.4502 26.9999 22.9024 27 27C26.9999 27.8283 26.3284 28.5 25.5 28.5C24.6716 28.5 24.0001 27.8283 24 27C23.9999 24.4977 22.0436 22.4502 19.4854 22.4502H12.5146C10.0486 22.4502 8.00013 24.4049 8 27C7.99993 27.8283 7.32839 28.5 6.5 28.5C5.67161 28.5 5.00007 27.8283 5 27C5.00013 22.7203 8.41967 19.4502 12.5146 19.4502H19.4854Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8428 5.56052C13.7449 2.81322 18.2044 2.8131 21.1064 5.56052C21.1133 5.56697 21.1193 5.57446 21.126 5.58103C21.13 5.585 21.1347 5.58873 21.1387 5.59275C24.0256 8.49691 24.0733 13.1802 21.1064 15.9892C18.2666 18.6778 13.6131 18.9546 10.7783 15.9248C8.10982 13.0726 7.83648 8.40684 10.8428 5.56052ZM19.0186 7.71579C17.2731 6.08723 14.6422 6.09502 12.9053 7.73923C11.2644 9.29282 11.2636 12.0522 12.9688 13.875C14.508 15.5201 17.2363 15.5218 19.0439 13.8105C20.7217 12.2218 20.771 9.48679 19.0186 7.71579Z"),
            )
        }.build()
        return _ic_user_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser32Preview() {
    Icon(
        imageVector = Icons.ic_user_32,
        contentDescription = null,
    )
}