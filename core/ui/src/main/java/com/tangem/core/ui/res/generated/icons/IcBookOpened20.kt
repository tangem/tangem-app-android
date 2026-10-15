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

private var _ic_book_opened_20: ImageVector? = null

val Icons.ic_book_opened_20: ImageVector
    get() {
        if (_ic_book_opened_20 != null) return _ic_book_opened_20!!
        _ic_book_opened_20 = ImageVector.Builder(
            name = "ic_book_opened_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.22168 3.5C8.15084 3.5 9.04616 3.85911 9.70898 4.50488C9.81338 4.6066 9.91016 4.71454 10 4.82715C10.6488 4.01676 11.6574 3.5 12.7783 3.5H15.209C15.6775 3.50009 16.1306 3.68144 16.4678 4.00977C16.8057 4.33899 16.9999 4.79043 17 5.26465V13.3828C16.9999 14.3737 16.1819 15.1452 15.2129 15.1475H12.249C11.571 15.1467 10.9618 15.5191 10.668 16.0918C10.5099 16.4 10.1619 16.5597 9.8252 16.4785C9.60667 16.4257 9.43112 16.28 9.33398 16.0898L9.33105 16.0918C9.03714 15.519 8.42815 15.1465 7.75 15.1475H4.79102C4.32221 15.1475 3.86854 14.9654 3.53125 14.6367C3.19352 14.3075 3.00003 13.8569 3 13.3828V5.26465C3 4.79038 3.19333 4.33904 3.53125 4.00977C3.86854 3.68113 4.32222 3.5 4.79102 3.5H7.22168ZM4.79102 5C4.70765 5 4.63129 5.03221 4.57812 5.08398C4.52562 5.13514 4.5 5.20093 4.5 5.26465V13.3828C4.50003 13.4464 4.5258 13.5114 4.57812 13.5625C4.63129 13.6143 4.70763 13.6475 4.79102 13.6475H7.74902C8.27942 13.6469 8.79254 13.7719 9.24902 14.0029V6.95605C9.24902 6.44391 9.03997 5.94839 8.66211 5.58008C8.2834 5.21109 7.76545 5 7.22168 5H4.79102ZM12.7783 5C11.6405 5 10.7511 5.89436 10.751 6.95605V14.0029C11.2075 13.772 11.7206 13.6468 12.251 13.6475H15.209C15.3878 13.647 15.4999 13.5103 15.5 13.3828V5.26465C15.4999 5.20101 15.4733 5.13508 15.4209 5.08398C15.3678 5.03254 15.292 5.00009 15.209 5H12.7783Z"),
            )
        }.build()
        return _ic_book_opened_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBookOpened20Preview() {
    Icon(
        imageVector = Icons.ic_book_opened_20,
        contentDescription = null,
    )
}