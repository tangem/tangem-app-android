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

private var _ic_user_plus_20: ImageVector? = null

val Icons.ic_user_plus_20: ImageVector
    get() {
        if (_ic_user_plus_20 != null) return _ic_user_plus_20!!
        _ic_user_plus_20 = ImageVector.Builder(
            name = "ic_user_plus_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.5723 11.375C10.9863 11.3753 11.3223 11.7109 11.3223 12.125C11.322 12.5388 10.9861 12.8747 10.5723 12.875H7.125C5.95158 12.875 5 13.8266 5 15V15.7188C4.99951 16.1325 4.66391 16.4688 4.25 16.4688C3.83609 16.4688 3.50049 16.1325 3.5 15.7188V15C3.5 12.9982 5.12315 11.375 7.125 11.375H10.5723Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.3408 10.6562C14.755 10.6562 15.0908 10.992 15.0908 11.4062V12.8125H16.4971C16.9111 12.8128 17.2471 13.1484 17.2471 13.5625C17.2468 13.9763 16.9109 14.3122 16.4971 14.3125H15.0908V15.7178C15.0906 16.1318 14.7549 16.4678 14.3408 16.4678C13.9268 16.4678 13.5911 16.1318 13.5908 15.7178V14.3125H12.1855C11.7715 14.3125 11.4358 13.9765 11.4355 13.5625C11.4355 13.1483 11.7713 12.8125 12.1855 12.8125H13.5908V11.4062C13.5908 10.992 13.9266 10.6562 14.3408 10.6562Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.2861 2.75C12.2878 2.75018 13.9102 4.37424 13.9102 6.37598C13.9097 8.37733 12.2876 9.99982 10.2861 10C8.28456 10 6.66158 8.37745 6.66113 6.37598C6.66113 4.37413 8.28429 2.75 10.2861 2.75ZM10.2861 4.25098C9.11271 4.25098 8.16113 5.20256 8.16113 6.37598C8.16158 7.54902 9.11299 8.5 10.2861 8.5C11.4591 8.49982 12.4097 7.54891 12.4102 6.37598C12.4102 5.20267 11.4594 4.25116 10.2861 4.25098Z"),
            )
        }.build()
        return _ic_user_plus_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus20Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_20,
        contentDescription = null,
    )
}