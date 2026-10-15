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

private var _ic_hanger_24: ImageVector? = null

val Icons.ic_hanger_24: ImageVector
    get() {
        if (_ic_hanger_24 != null) return _ic_hanger_24!!
        _ic_hanger_24 = ImageVector.Builder(
            name = "ic_hanger_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C13.6573 2 15 3.34272 15 5C15 6.32549 14.1276 7.02079 13.5703 7.39648L13.5684 7.39746C13.3448 7.54775 13.2058 7.66735 13.1191 7.78418C13.0726 7.84685 13.0374 7.91604 13.0176 8.00781C13.85 8.08514 14.6759 8.25604 15.4785 8.52344L19.7266 9.93945C21.0849 10.3917 21.9999 11.6618 22 13.0928C21.9999 14.8191 20.6844 16.2383 19 16.4014V18.9512C18.9999 20.6343 17.6343 21.9999 15.9512 22H8.04883C6.36568 21.9999 5.00009 20.6343 5 18.9512V16.4082C3.32077 16.2713 2.00028 14.8668 2 13.1523V13.1025C2.00019 11.6643 2.91275 10.3925 4.27246 9.93945L8.52051 8.52344C9.33095 8.25342 10.1651 8.08109 11.0059 8.00488C11.0734 6.76729 11.906 6.10495 12.4531 5.7373C12.6708 5.59048 12.8048 5.47505 12.8877 5.36328C12.9541 5.27377 13 5.17134 13 5C13 4.44728 12.5527 4 12 4C11.4819 4 11.0561 4.39299 11.0049 4.89746L10.9951 5.10254C10.9438 5.60667 10.5177 6 10 6C9.44772 6 9 5.55228 9 5C9 3.34272 10.3427 2 12 2ZM7.01562 15.002C7.01337 15.0029 7.01075 15.0049 7.00781 15.0078C7.00488 15.0107 7.00294 15.0134 7.00195 15.0156C7.00127 15.0172 7 15.0202 7 15.0254V18.9512C7.00009 19.5298 7.47025 19.9999 8.04883 20H15.9512C16.5298 19.9999 16.9999 19.5298 17 18.9512V15.0254C17 15.0202 16.9988 15.0173 16.998 15.0156C16.997 15.0133 16.9952 15.0098 16.9922 15.0068C16.9893 15.004 16.9864 15.0018 16.9844 15.001C16.9829 15.0004 16.9802 15 16.9756 15H7.02539C7.02017 15 7.01723 15.0013 7.01562 15.002ZM14.8457 10.4209C12.998 9.80532 11.0011 9.80532 9.15332 10.4209L4.90527 11.8369C4.36767 12.016 4.00019 12.5194 4 13.1025V13.1523C4.00027 13.794 4.47723 14.3226 5.0957 14.4072C5.35709 13.591 6.12262 13 7.02539 13H16.9756C17.8739 13 18.6347 13.5864 18.8994 14.3965C19.5239 14.29 19.9999 13.7485 20 13.0928C19.9999 12.5224 19.6355 12.017 19.0947 11.8369L14.8457 10.4209Z"),
            )
        }.build()
        return _ic_hanger_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHanger24Preview() {
    Icon(
        imageVector = Icons.ic_hanger_24,
        contentDescription = null,
    )
}