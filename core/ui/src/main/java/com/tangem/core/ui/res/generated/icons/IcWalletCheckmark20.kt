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

private var _ic_wallet_checkmark_20: ImageVector? = null

val Icons.ic_wallet_checkmark_20: ImageVector
    get() {
        if (_ic_wallet_checkmark_20 != null) return _ic_wallet_checkmark_20!!
        _ic_wallet_checkmark_20 = ImageVector.Builder(
            name = "ic_wallet_checkmark_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.2399 3.50732L13.2389 3.5083C13.3461 3.5083 13.4199 3.5079 13.485 3.51025C15.2747 3.57506 16.7113 5.01054 16.776 6.80029C16.7771 6.83159 16.7758 7.00043 16.776 7.18506C17.5324 7.32207 18.0006 8.04351 18.0006 8.74756V11.2583C18.0004 11.9621 17.5321 12.6818 16.776 12.8188C16.7756 12.9985 16.7762 13.1711 16.775 13.2046C16.7104 14.9944 15.2748 16.4308 13.485 16.4956C13.4199 16.498 13.3461 16.4976 13.2389 16.4976H10.694C10.2798 16.4975 9.94397 16.1618 9.94397 15.7476C9.94417 15.3335 10.2799 14.9976 10.694 14.9976H13.2389C13.3588 14.9976 13.3992 14.9977 13.4303 14.9966C14.4343 14.9603 15.2396 14.1547 15.276 13.1509C15.276 13.1472 15.2767 13.1147 15.277 13.0464C15.2772 12.9898 15.277 12.9194 15.277 12.8452H14.6783C13.161 12.8452 12.0907 11.4775 12.0905 10.0024C12.0907 8.52736 13.161 7.16064 14.6783 7.16064H15.278C15.2777 7.09129 15.2772 7.02606 15.277 6.97217C15.2767 6.89915 15.2769 6.86182 15.277 6.85498C15.2407 5.85107 14.4343 5.04561 13.4303 5.00928C13.3992 5.00816 13.3587 5.00732 13.2389 5.00732L7.01526 5.00244C6.25666 5.00244 5.73267 5.0034 5.32581 5.03662C4.92758 5.06916 4.70736 5.12897 4.54553 5.21143C4.1853 5.3951 3.89226 5.68811 3.70862 6.04834C3.62626 6.21012 3.56633 6.43069 3.53381 6.82861C3.50063 7.23542 3.49963 7.75973 3.49963 8.51807C3.49937 8.93202 3.16364 9.26801 2.74963 9.26807C2.33558 9.26807 1.9999 8.93206 1.99963 8.51807C1.99963 7.78441 1.99937 7.18855 2.0387 6.70654C2.0788 6.21575 2.16371 5.77607 2.3717 5.36768C2.69922 4.72492 3.22211 4.20203 3.86487 3.87451C4.27338 3.66638 4.71276 3.58162 5.20374 3.5415C5.68581 3.50214 6.28139 3.50244 7.01526 3.50244L13.2399 3.50732ZM14.6783 8.66064C14.1664 8.66064 13.5907 9.16693 13.5905 10.0024C13.5907 10.8379 14.1664 11.3452 14.6783 11.3452H16.4772C16.4869 11.3298 16.5005 11.301 16.5006 11.2583V8.74756C16.5006 8.70504 16.4869 8.6763 16.4772 8.66064H14.6783Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.12659 11.5874C8.4194 11.2946 8.89422 11.2947 9.18713 11.5874C9.48003 11.8803 9.48003 12.3551 9.18713 12.6479L5.65198 16.1841C5.35906 16.4767 4.88326 16.4769 4.59045 16.1841L2.46936 14.062C2.17681 13.7692 2.17682 13.2943 2.46936 13.0015C2.76218 12.7088 3.23705 12.7089 3.52991 13.0015L5.12073 14.5923L8.12659 11.5874Z"),
            )
        }.build()
        return _ic_wallet_checkmark_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark20Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_20,
        contentDescription = null,
    )
}