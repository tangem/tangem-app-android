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

private var _ic_wallet_20: ImageVector? = null

val Icons.ic_wallet_20: ImageVector
    get() {
        if (_ic_wallet_20 != null) return _ic_wallet_20!!
        _ic_wallet_20 = ImageVector.Builder(
            name = "ic_wallet_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.5439 3C16.8835 3.00026 18 4.06618 18 5.41699V14.583C17.9996 15.9335 16.8833 16.9988 15.5439 16.999H4.45703C3.1175 16.999 2.00037 15.9337 2 14.583V5.41699C2 4.06602 3.11728 3 4.45703 3H15.5439ZM3.50098 14.583C3.50135 15.0726 3.91289 15.499 4.45703 15.499H15.5439C16.0878 15.4988 16.4996 15.0724 16.5 14.583V13.666H14.6914C13.3519 13.666 12.2358 12.6006 12.2354 11.25C12.2354 9.89902 13.3517 8.83301 14.6914 8.83301H16.5V7.83301H4.45703C4.11954 7.83301 3.79581 7.76437 3.50098 7.6416V14.583ZM14.6914 10.333C14.147 10.333 13.7354 10.7601 13.7354 11.25C13.7358 11.7395 14.1473 12.166 14.6914 12.166H16.5V10.333H14.6914ZM4.45703 4.5C3.91264 4.5 3.50098 4.92713 3.50098 5.41699L3.50586 5.50781C3.55335 5.95804 3.9469 6.33301 4.45703 6.33301H16.5V5.41699C16.5 4.92728 16.0881 4.50026 15.5439 4.5H4.45703Z"),
            )
        }.build()
        return _ic_wallet_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet20Preview() {
    Icon(
        imageVector = Icons.ic_wallet_20,
        contentDescription = null,
    )
}