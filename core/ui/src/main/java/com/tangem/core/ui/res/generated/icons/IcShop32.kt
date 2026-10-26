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

private var _ic_shop_32: ImageVector? = null

val Icons.ic_shop_32: ImageVector
    get() {
        if (_ic_shop_32 != null) return _ic_shop_32!!
        _ic_shop_32 = ImageVector.Builder(
            name = "ic_shop_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.6816 3C25.4726 3 27.0518 4.17519 27.5664 5.89062L28.8291 10.0986L28.9033 10.3848C28.9246 10.4807 28.9408 10.5776 28.9551 10.6748C28.9586 10.6991 28.9625 10.7234 28.9648 10.748C28.9868 10.9189 29 11.0909 29 11.2637V13.4443C29 15.0892 28.2545 16.5588 27.084 17.5371V24.9453C27.0837 27.1848 25.2678 28.9998 23.0283 29H8.97266C6.73298 29 4.91724 27.1849 4.91699 24.9453V17.5381C3.7459 16.5598 3 15.0896 3 13.4443V11.2637C3.00001 11.0926 3.0127 10.9222 3.03418 10.7529C3.03544 10.7389 3.03644 10.7249 3.03809 10.7109C3.04272 10.6773 3.04921 10.6439 3.05469 10.6104C3.05677 10.5993 3.05822 10.5881 3.06055 10.5771C3.08825 10.4158 3.12375 10.2558 3.1709 10.0986L4.43359 5.89062C4.94823 4.17519 6.52739 3 8.31836 3H23.6816ZM19.832 17.1504C18.8623 18.1527 17.5047 18.7772 16 18.7773C14.4951 18.7773 13.1366 18.1529 12.167 17.1504C11.2564 18.0919 10.0031 18.6999 8.60742 18.7705L8.33301 18.7783L8.05859 18.7705C8.01118 18.7681 7.96407 18.7634 7.91699 18.7598V24.9453C7.91724 25.5281 8.38985 26 8.97266 26H23.0283C23.6109 25.9998 24.0837 25.5279 24.084 24.9453V18.7598C24.0366 18.7634 23.9891 18.7681 23.9414 18.7705L23.667 18.7783L23.3926 18.7705C21.9967 18.6998 20.7426 18.0922 19.832 17.1504ZM6 13.4443C6 14.7329 7.0445 15.7781 8.33301 15.7783C9.62167 15.7783 10.667 14.733 10.667 13.4443V12.3887H6V13.4443ZM21.334 13.4443C21.334 14.733 22.3783 15.7783 23.667 15.7783C24.9555 15.7782 26 14.7329 26 13.4443V12.3887H21.334V13.4443ZM13.667 13.4443C13.667 14.7329 14.7115 15.7772 16 15.7773C17.2846 15.7771 18.3268 14.7393 18.333 13.4561V12.3887H13.667V13.4443ZM8.31836 6C7.85228 6 7.44064 6.30555 7.30664 6.75195L6.5166 9.38867H25.4834L24.6934 6.75195C24.5594 6.30555 24.1477 6 23.6816 6H8.31836Z"),
            )
        }.build()
        return _ic_shop_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShop32Preview() {
    Icon(
        imageVector = Icons.ic_shop_32,
        contentDescription = null,
    )
}