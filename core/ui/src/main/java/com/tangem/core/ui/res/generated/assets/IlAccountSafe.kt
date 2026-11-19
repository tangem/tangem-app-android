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

private var _il_account_safe: ImageVector? = null

val Icons.il_account_safe: ImageVector
    get() {
        if (_il_account_safe != null) return _il_account_safe!!
        _il_account_safe = ImageVector.Builder(
            name = "il_account_safe",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M33 18.7596C32.9998 16.6834 31.3168 15 29.2406 15H18.7594C16.6835 15.0003 15.0001 16.6835 15 18.7596V27.4076C15 29.1339 16.1644 30.5868 17.75 31.0282V32.2481C17.75 32.6633 18.0867 33 18.5019 33C18.917 32.9999 19.2538 32.6633 19.2538 32.2481V31.1672H28.7472V32.2481C28.7472 32.6633 29.0838 33 29.4991 33C29.9143 32.9999 30.251 32.6633 30.251 32.2481V31.0282C31.8363 30.5867 33 29.1338 33 27.4076V18.7596ZM24.7519 19.4175V20.61C25.5748 20.8598 26.2233 21.5091 26.473 22.3322H27.6664C28.0814 22.3324 28.4182 22.669 28.4183 23.0841C28.418 23.499 28.0813 23.8358 27.6664 23.836H26.472C26.222 24.6584 25.5743 25.3065 24.7519 25.5562V26.7497C24.7517 27.1648 24.4152 27.5016 24 27.5016C23.585 27.5014 23.2483 27.1647 23.2481 26.7497V25.5562C22.4258 25.3064 21.778 24.6583 21.528 23.836H20.3346C19.9195 23.836 19.583 23.4991 19.5827 23.0841C19.5827 22.6688 19.9194 22.3322 20.3346 22.3322H21.527C21.7767 21.5092 22.4253 20.8599 23.2481 20.61V19.4175C23.2482 19.0024 23.585 18.6658 24 18.6656C24.4152 18.6656 24.7518 19.0023 24.7519 19.4175ZM24 22.0022C23.4033 22.0024 22.9195 22.4864 22.9192 23.0831C22.9192 23.6801 23.4031 24.1638 24 24.164C24.597 24.164 25.0808 23.6802 25.0808 23.0831C25.0805 22.4863 24.5968 22.0022 24 22.0022Z"),
            )
        }.build()
        return _il_account_safe!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountSafePreview() {
    Icon(
        imageVector = Icons.il_account_safe,
        contentDescription = null,
    )
}