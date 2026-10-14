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

private var _ic_wallet_plus_16: ImageVector? = null

val Icons.ic_wallet_plus_16: ImageVector
    get() {
        if (_ic_wallet_plus_16 != null) return _ic_wallet_plus_16!!
        _ic_wallet_plus_16 = ImageVector.Builder(
            name = "ic_wallet_plus_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.3208 3.00098C11.8007 3.05455 12.9878 4.23535 13.0542 5.71191C13.6402 5.85114 13.9985 6.42565 13.9985 6.9834V9.01563C13.9984 9.57248 13.6406 10.1432 13.0562 10.2832C12.992 11.7618 11.8022 12.9453 10.3208 12.999C10.2669 13.001 10.2054 13.001 10.1167 13.001H8.00147C7.65644 13.001 7.37671 12.7209 7.37647 12.376C7.37661 12.0309 7.65638 11.751 8.00147 11.751H10.1167C10.2161 11.751 10.2501 11.7509 10.2759 11.75C11.0758 11.7209 11.7226 11.1035 11.8003 10.3174H11.2905C10.0504 10.3171 9.17833 9.20067 9.17822 7.99902C9.17834 6.79739 10.0504 5.68096 11.2905 5.68066H11.7993C11.7208 4.89542 11.0752 4.27908 10.2759 4.25C10.25 4.24907 10.2155 4.24902 10.1157 4.24902H6.17236C5.54182 4.24902 5.10618 4.24975 4.76807 4.27734C4.43739 4.30437 4.25482 4.35451 4.12061 4.42285C3.82126 4.57539 3.57781 4.81888 3.42529 5.11816C3.35698 5.25229 3.30682 5.43423 3.27979 5.76465C3.25215 6.10279 3.25147 6.53912 3.25147 7.16992C3.25125 7.51492 2.97151 7.79492 2.62647 7.79492C2.28152 7.79481 2.00168 7.51485 2.00147 7.16992C2.00147 6.55975 2.00094 6.06394 2.03369 5.66309C2.06705 5.25502 2.13791 4.88955 2.31104 4.54981C2.58343 4.01544 3.01878 3.5809 3.55322 3.30859C3.89278 3.13569 4.25771 3.06461 4.66553 3.03125C5.06637 2.99851 5.56221 2.99902 6.17236 2.99902H10.1157C10.2048 2.99902 10.2667 2.99903 10.3208 3.00098ZM11.2905 6.93066C10.888 6.93099 10.4283 7.33063 10.4282 7.99902C10.4283 8.66744 10.888 9.06705 11.2905 9.06738H12.7378C12.7428 9.05624 12.7485 9.03907 12.7485 9.01563V6.9834C12.7485 6.95924 12.742 6.94185 12.7368 6.93066H11.2905Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.61768 7.87793C4.96264 7.878 5.24243 8.15801 5.24268 8.50293V9.7832H6.52295C6.86785 9.78334 7.1477 10.0633 7.14795 10.4082C7.14786 10.7532 6.86795 11.0331 6.52295 11.0332H5.24268V12.3125C5.24257 12.6575 4.96273 12.9374 4.61768 12.9375C4.27276 12.9373 3.99278 12.6574 3.99268 12.3125V11.0332H2.71338C2.36826 11.0332 2.08847 10.7533 2.08838 10.4082C2.08863 10.0632 2.36836 9.7832 2.71338 9.7832H3.99268V8.50293C3.99293 8.15811 4.27285 7.87816 4.61768 7.87793Z"),
            )
        }.build()
        return _ic_wallet_plus_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletPlus16Preview() {
    Icon(
        imageVector = Icons.ic_wallet_plus_16,
        contentDescription = null,
    )
}