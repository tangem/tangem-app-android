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

private var _ic_headphones_support_24: ImageVector? = null

val Icons.ic_headphones_support_24: ImageVector
    get() {
        if (_ic_headphones_support_24 != null) return _ic_headphones_support_24!!
        _ic_headphones_support_24 = ImageVector.Builder(
            name = "ic_headphones_support_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C15.9698 2 19.2393 5.05068 19.4834 8.93262C20.9001 9.16069 21.9998 10.3705 22 11.8594V14.8125C22 16.4673 20.6424 17.7812 19 17.7812C18.9919 19.4289 17.6373 20.7351 16 20.7354H14.6699C14.2998 21.4877 13.5214 21.9998 12.6338 22H11.3672C10.1302 22 9.10089 21.0078 9.10059 19.7529C9.10059 18.4978 10.1301 17.5049 11.3672 17.5049H12.6338C13.5077 17.5051 14.2755 18.0018 14.6523 18.7354H16C16.5672 18.7351 16.9999 18.2871 17 17.7666V17.5127C16.4066 17.1717 16 16.5378 16 15.7969V10.875C16.0002 9.94746 16.6361 9.1868 17.4805 8.96094C17.2537 6.19625 14.9009 4 12 4C9.09903 4 6.74527 6.19622 6.51855 8.96094C7.36333 9.18649 7.99984 9.94712 8 10.875V15.7969C8 16.9073 7.08943 17.7812 6 17.7812H5C3.35757 17.7812 2 16.4673 2 14.8125V11.8594C2.0002 10.3708 3.09934 9.16108 4.51562 8.93262C4.75971 5.05065 8.0302 2 12 2ZM11.3672 19.5049C11.206 19.5049 11.1006 19.6308 11.1006 19.7529C11.1009 19.8749 11.2062 20 11.3672 20H12.6338C12.7945 19.9997 12.9001 19.8748 12.9004 19.7529C12.9004 19.6309 12.7948 19.5051 12.6338 19.5049H11.3672ZM5 10.8906C4.43257 10.8906 4.00022 11.3388 4 11.8594V14.8125C4 15.3333 4.43243 15.7812 5 15.7812H6V10.8906H5ZM18 15.7812H19C19.5676 15.7812 20 15.3333 20 14.8125V11.8594C19.9998 11.3388 19.5674 10.8906 19 10.8906H18V15.7812Z"),
            )
        }.build()
        return _ic_headphones_support_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeadphonesSupport24Preview() {
    Icon(
        imageVector = Icons.ic_headphones_support_24,
        contentDescription = null,
    )
}