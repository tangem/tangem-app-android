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

private var _ic_wallet_28: ImageVector? = null

val Icons.ic_wallet_28: ImageVector
    get() {
        if (_ic_wallet_28 != null) return _ic_wallet_28!!
        _ic_wallet_28 = ImageVector.Builder(
            name = "ic_wallet_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.4561 4C23.4333 4.00015 25 5.62509 25 7.58398V20.4189C24.9997 22.3776 23.4332 24.0028 21.4561 24.0029H6.54395C4.56679 24.0028 3.00027 22.3776 3 20.4189V7.58398C3 5.62506 4.56662 4.0001 6.54395 4H21.4561ZM5.5 20.4189C5.50027 21.0373 5.98757 21.5028 6.54395 21.5029H21.4561C22.0124 21.5028 22.4997 21.0373 22.5 20.4189V19.334H20.3086C18.3315 19.3337 16.7648 17.7097 16.7646 15.751C16.7649 13.7923 18.3315 12.1672 20.3086 12.167H22.5V11.167H6.54395C6.17972 11.167 5.82913 11.112 5.5 11.0098V20.4189ZM20.3086 14.667C19.7523 14.6672 19.2649 15.1327 19.2646 15.751C19.2648 16.3694 19.7523 16.8337 20.3086 16.834H22.5V14.667H20.3086ZM6.54395 6.5C5.98741 6.50011 5.5 6.96534 5.5 7.58398L5.50586 7.69824C5.56185 8.2586 6.02248 8.66689 6.54395 8.66699H22.5V7.58398C22.5 6.96537 22.0126 6.50015 21.4561 6.5H6.54395Z"),
            )
        }.build()
        return _ic_wallet_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet28Preview() {
    Icon(
        imageVector = Icons.ic_wallet_28,
        contentDescription = null,
    )
}