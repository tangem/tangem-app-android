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

private var _ic_wallet_checkmark_16: ImageVector? = null

val Icons.ic_wallet_checkmark_16: ImageVector
    get() {
        if (_ic_wallet_checkmark_16 != null) return _ic_wallet_checkmark_16!!
        _ic_wallet_checkmark_16 = ImageVector.Builder(
            name = "ic_wallet_checkmark_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.3198 3.00195C11.7997 3.05553 12.9867 4.23632 13.0532 5.71289C13.6392 5.85212 13.9975 6.42662 13.9975 6.98438V9.0166C13.9973 9.57346 13.6396 10.1441 13.0551 10.2842C12.9909 11.7628 11.8011 12.9463 10.3198 13C10.2658 13.0019 10.2043 13.002 10.1157 13.002H8.00043C7.6554 13.002 7.37567 12.7219 7.37543 12.377C7.37558 12.0319 7.65534 11.752 8.00043 11.752H10.1157C10.2151 11.752 10.249 11.7519 10.2748 11.751C11.0748 11.7219 11.7215 11.1045 11.7993 10.3184H11.2895C10.0494 10.3181 9.17729 9.20164 9.17719 8C9.1773 6.79837 10.0494 5.68194 11.2895 5.68164H11.7983C11.7198 4.8964 11.0741 4.28006 10.2748 4.25098C10.249 4.25005 10.2145 4.25 10.1147 4.25H6.17133C5.54078 4.25 5.10514 4.25072 4.76703 4.27832C4.43636 4.30535 4.25378 4.35548 4.11957 4.42383C3.82022 4.57637 3.57677 4.81986 3.42426 5.11914C3.35594 5.25327 3.30578 5.43521 3.27875 5.76563C3.25112 6.10376 3.25043 6.5401 3.25043 7.1709C3.25022 7.5159 2.97048 7.7959 2.62543 7.7959C2.28048 7.79578 2.00064 7.51583 2.00043 7.1709C2.00043 6.56072 1.9999 6.06492 2.03265 5.66406C2.06601 5.25599 2.13688 4.89052 2.31 4.55078C2.5824 4.01641 3.01775 3.58188 3.55219 3.30957C3.89174 3.13667 4.25667 3.06558 4.66449 3.03223C5.06533 2.99948 5.56117 3 6.17133 3H10.1147C10.2037 3 10.2657 3.00001 10.3198 3.00195ZM11.2895 6.93164C10.887 6.93197 10.4273 7.3316 10.4272 8C10.4273 8.66842 10.887 9.06803 11.2895 9.06836H12.7368C12.7418 9.05722 12.7474 9.04005 12.7475 9.0166V6.98438C12.7475 6.96022 12.741 6.94282 12.7358 6.93164H11.2895Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.90179 9.51953C6.14589 9.27622 6.54174 9.27583 6.78558 9.51953C7.02952 9.76349 7.02928 10.1602 6.78558 10.4043L4.51117 12.6777C4.2671 12.9218 3.87146 12.9218 3.62738 12.6777L2.26215 11.3135C2.01842 11.0694 2.01837 10.6737 2.26215 10.4297C2.50626 10.186 2.90296 10.1857 3.14691 10.4297L4.06879 11.3516L5.90179 9.51953Z"),
            )
        }.build()
        return _ic_wallet_checkmark_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark16Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_16,
        contentDescription = null,
    )
}