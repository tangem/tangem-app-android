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

private var _ic_wallet_checkmark_12: ImageVector? = null

val Icons.ic_wallet_checkmark_12: ImageVector
    get() {
        if (_ic_wallet_checkmark_12 != null) return _ic_wallet_checkmark_12!!
        _ic_wallet_checkmark_12 = ImageVector.Builder(
            name = "ic_wallet_checkmark_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.96692 2.50101C8.98582 2.53651 9.83095 3.32964 9.86927 4.3516C9.8696 4.36058 9.86911 4.38976 9.86927 4.42972C10.2562 4.55624 10.5001 4.94085 10.5001 5.33207V6.67094C10.5 7.06238 10.2557 7.44703 9.86829 7.57328C9.86805 7.61054 9.86863 7.63879 9.86829 7.64847C9.82975 8.67008 8.98547 9.46335 7.96692 9.49906C7.92952 9.50035 7.88674 9.50004 7.82825 9.50004H6.42102C6.14516 9.49991 5.92129 9.27587 5.92102 9.00004C5.92127 8.72419 6.14515 8.50017 6.42102 8.50004H7.93177C8.45522 8.48176 8.84744 8.08219 8.86829 7.61722H8.57727C7.6972 7.61705 7.06165 6.84812 7.06165 6.00101C7.06183 5.15405 7.69733 4.38596 8.57727 4.38578H8.87024C8.85099 3.91947 8.45642 3.51929 7.93177 3.50101C7.9161 3.50048 7.89486 3.50004 7.82825 3.50004H4.35852C3.93793 3.50004 3.65138 3.50016 3.42981 3.51761C3.21362 3.53465 3.10258 3.56571 3.02552 3.60355C2.84067 3.69438 2.69357 3.83777 2.60266 4.0098C2.56663 4.078 2.53603 4.17848 2.51868 4.38285C2.5008 4.59385 2.50013 4.86799 2.50013 5.27445C2.50013 5.55059 2.27627 5.77445 2.00013 5.77445C1.72398 5.77445 1.50013 5.55059 1.50013 5.27445C1.50013 4.8851 1.49934 4.56184 1.52161 4.29886C1.54444 4.02947 1.5943 3.77882 1.71888 3.54301C1.9106 3.18016 2.21432 2.88786 2.58411 2.70609C2.82239 2.58902 3.07634 2.54322 3.35169 2.52152C3.62158 2.50028 3.95398 2.50004 4.35852 2.50004H7.82825C7.88694 2.50004 7.92944 2.49971 7.96692 2.50101ZM8.57727 5.38578C8.33549 5.38597 8.06182 5.61634 8.06165 6.00101C8.06165 6.3859 8.33542 6.61703 8.57727 6.61722H9.50013V5.38578H8.57727Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.7052 6.78617C4.90401 6.59527 5.22079 6.60136 5.41224 6.79984C5.60333 6.99849 5.59773 7.31528 5.39954 7.50687L3.54212 9.29691C3.34845 9.48372 3.04149 9.48366 2.84778 9.29691L1.7345 8.22269C1.53581 8.03104 1.5293 7.71443 1.72083 7.51566C1.91249 7.31739 2.22923 7.31163 2.42786 7.50297L3.19446 8.24222L4.7052 6.78617Z"),
            )
        }.build()
        return _ic_wallet_checkmark_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark12Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_12,
        contentDescription = null,
    )
}