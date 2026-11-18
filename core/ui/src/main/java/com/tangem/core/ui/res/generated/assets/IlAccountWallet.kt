@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_wallet: ImageVector? = null

val Icons.il_account_wallet: ImageVector
    get() {
        if (_il_account_wallet != null) return _il_account_wallet!!
        _il_account_wallet = ImageVector.Builder(
            name = "il_account_wallet",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.2246 15.4467C25.4097 14.6639 27.0068 14.9349 27.8615 16.0639L29.6218 18.3895C31.0646 18.6544 32.1572 19.9073 32.1574 21.4136V22.3859C32.6638 22.7112 33 23.2758 33 23.9183V26.421C32.9999 27.0633 32.6637 27.6271 32.1574 27.9524V28.9236C32.1574 30.6228 30.7675 32 29.0526 32H18.1048C16.39 31.9998 15 30.6227 15 28.9236V21.4136C15.0002 19.7147 16.3902 18.3374 18.1048 18.3373H19.8486L24.2246 15.4467ZM29.0526 24.074C28.4423 24.0742 27.9476 24.5644 27.9474 25.1691C27.9476 25.7738 28.4423 26.265 29.0526 26.2652H31.0004V24.074H29.0526ZM26.2612 17.2528C26.0433 16.9651 25.6359 16.8957 25.3337 17.0951L23.4532 18.3373H27.0823L26.2612 17.2528Z"),
            )
        }.build()
        return _il_account_wallet!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountWalletPreview() {
    Icon(
        imageVector = Icons.il_account_wallet,
        contentDescription = null,
    )
}