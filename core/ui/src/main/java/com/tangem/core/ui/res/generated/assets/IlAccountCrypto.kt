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

private var _il_account_crypto: ImageVector? = null

val Icons.il_account_crypto: ImageVector
    get() {
        if (_il_account_crypto != null) return _il_account_crypto!!
        _il_account_crypto = ImageVector.Builder(
            name = "il_account_crypto",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color.White),
                fillAlpha = 0.8f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M27.3212 15.1992C30.0086 15.1992 31.3526 15.1988 32.3792 15.7216C33.2821 16.1817 34.0164 16.9161 34.4765 17.819C34.9996 18.8456 34.9999 20.19 34.9999 22.8779V25.1227C34.9999 27.8104 34.9996 29.154 34.4765 30.1806C34.0164 31.0837 33.2822 31.8179 32.3792 32.278C31.3526 32.801 30.0088 32.8014 27.3212 32.8014H20.6786C17.991 32.8014 16.6472 32.801 15.6207 32.278C14.7176 31.8179 13.9834 31.0837 13.5233 30.1806C13.0002 29.154 12.9999 27.8103 12.9999 25.1227V22.8779C12.9999 20.19 13.0002 18.8456 13.5233 17.819C13.9834 16.9161 14.7177 16.1817 15.6207 15.7216C16.6472 15.1987 17.9912 15.1992 20.6786 15.1992H27.3212ZM29.2341 21.6007C27.9088 21.6007 26.8341 22.6746 26.834 23.9998C26.834 25.3251 27.9088 26.3999 29.2341 26.3999C30.5593 26.3998 31.6332 25.325 31.6332 23.9998C31.6331 22.6746 30.5592 21.6008 29.2341 21.6007Z"),
            )
        }.build()
        return _il_account_crypto!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountCryptoPreview() {
    Icon(
        imageVector = Icons.il_account_crypto,
        contentDescription = null,
    )
}