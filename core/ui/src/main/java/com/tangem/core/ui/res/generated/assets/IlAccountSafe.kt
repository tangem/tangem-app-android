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
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.957 6.79297C20.9569 4.72203 19.278 3.04297 17.207 3.04297H6.75195C4.68122 3.04325 3.0021 4.7222 3.00195 6.79297V15.4189C3.00195 17.1409 4.16347 18.59 5.74512 19.0303V20.2471C5.74512 20.6613 6.08092 20.997 6.49512 20.9971C6.90922 20.9969 7.24512 20.6612 7.24512 20.2471V19.1689H16.7148V20.2471C16.7148 20.6613 17.0507 20.997 17.4648 20.9971C17.879 20.997 18.2148 20.6612 18.2148 20.2471V19.0303C19.7963 18.5899 20.957 17.1408 20.957 15.4189V6.79297ZM12.7295 7.44922V8.63867C13.5504 8.88784 14.1972 9.53549 14.4463 10.3564H15.6367C16.0507 10.3567 16.3867 10.6924 16.3867 11.1064C16.3865 11.5203 16.0506 11.8562 15.6367 11.8564H14.4453C14.196 12.6767 13.5499 13.3232 12.7295 13.5723V14.7627C12.7294 15.1768 12.3936 15.5127 11.9795 15.5127C11.5655 15.5125 11.2296 15.1767 11.2295 14.7627V13.5723C10.4092 13.3231 9.763 12.6766 9.51367 11.8564H8.32324C7.90919 11.8564 7.57351 11.5204 7.57324 11.1064C7.57327 10.6923 7.90904 10.3564 8.32324 10.3564H9.5127C9.76176 9.53559 10.4087 8.88795 11.2295 8.63867V7.44922C11.2296 7.0352 11.5655 6.6994 11.9795 6.69922C12.3936 6.69922 12.7294 7.03509 12.7295 7.44922ZM11.9795 10.0273C11.3843 10.0275 10.9017 10.5103 10.9014 11.1055C10.9014 11.7009 11.3841 12.1834 11.9795 12.1836C12.575 12.1836 13.0576 11.701 13.0576 11.1055C13.0573 10.5102 12.5748 10.0273 11.9795 10.0273Z"),
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