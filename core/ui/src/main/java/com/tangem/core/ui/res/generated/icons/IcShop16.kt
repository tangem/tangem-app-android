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

private var _ic_shop_16: ImageVector? = null

val Icons.ic_shop_16: ImageVector
    get() {
        if (_ic_shop_16 != null) return _ic_shop_16!!
        _ic_shop_16 = ImageVector.Builder(
            name = "ic_shop_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.9229 3C11.628 3.00018 12.2495 3.46321 12.4521 4.13867L12.9326 5.73926C12.9773 5.88814 13 6.0428 13 6.19824V7.02734C13 7.65992 12.7166 8.22514 12.2715 8.60742V11.4033C12.2713 12.2851 11.5556 12.9997 10.6738 13H5.32715C4.44515 13 3.72968 12.2853 3.72949 11.4033V8.6084C3.28382 8.22611 3 7.66043 3 7.02734V6.19824C3 6.04279 3.02271 5.88815 3.06738 5.73926L3.54785 4.13867C3.7505 3.4632 4.37196 3.00016 5.07715 3H10.9229ZM9.45801 8.5127C9.12959 8.83518 8.69649 9.05147 8.21289 9.10059L8 9.11133L7.78711 9.10059C7.30336 9.05142 6.86937 8.83539 6.54102 8.5127C6.21253 8.83521 5.77952 9.05158 5.2959 9.10059L5.08301 9.11133L4.97949 9.10547V11.4033C4.97968 11.5949 5.1355 11.75 5.32715 11.75H10.6738C10.8653 11.7497 11.0213 11.5948 11.0215 11.4033V9.10547L10.917 9.11133L10.7041 9.10059C10.2202 9.05156 9.78655 8.83548 9.45801 8.5127ZM4.25 7.02734C4.25 7.48746 4.62292 7.86116 5.08301 7.86133C5.54325 7.86133 5.91699 7.48757 5.91699 7.02734V6.68066H4.25V7.02734ZM7.16699 7.02832C7.16723 7.48823 7.54006 7.86116 8 7.86133C8.45726 7.86107 8.82857 7.49239 8.83301 7.03613V6.68066H7.16699V7.02832ZM10.084 7.02832C10.084 7.03158 10.083 7.03483 10.083 7.03809C10.0888 7.49337 10.4603 7.86133 10.917 7.86133C11.3771 7.86113 11.75 7.48744 11.75 7.02734V6.68066H10.084V7.02832ZM5.07715 4.25C4.9241 4.25016 4.78926 4.35054 4.74512 4.49707L4.46484 5.43066H11.5352L11.2549 4.49707C11.2107 4.35055 11.0759 4.25018 10.9229 4.25H5.07715Z"),
            )
        }.build()
        return _ic_shop_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShop16Preview() {
    Icon(
        imageVector = Icons.ic_shop_16,
        contentDescription = null,
    )
}