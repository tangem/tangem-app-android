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

private var _il_account_airplane: ImageVector? = null

val Icons.il_account_airplane: ImageVector
    get() {
        if (_il_account_airplane != null) return _il_account_airplane!!
        _il_account_airplane = ImageVector.Builder(
            name = "il_account_airplane",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 3.02637C13.3187 3.02647 14.3944 4.10223 14.3945 5.4209V8.88086L20.3076 12.7715L20.3115 12.7744C20.8236 13.1161 20.9735 13.6363 20.9736 14.0557V15.4541C20.9736 15.8964 20.7418 16.2487 20.4316 16.4463C20.1413 16.6311 19.7631 16.6944 19.4111 16.5771C19.3897 16.57 19.3683 16.5618 19.3477 16.5527L14.3945 14.3809V16.8438L15.623 17.9365L15.6553 17.9668C15.7456 18.0571 16.04 18.3323 16.04 18.8262V20.2236C16.04 20.4545 15.933 20.6733 15.751 20.8154C15.569 20.9573 15.3313 21.0071 15.1074 20.9512L12 20.1738L8.89258 20.9512C8.66865 21.0072 8.43102 20.9574 8.24902 20.8154C8.06703 20.6733 7.96099 20.4545 7.96094 20.2236V18.7441C7.96094 18.5214 8.04846 18.3289 8.09082 18.2441C8.1449 18.136 8.22952 18 8.34473 17.8848L8.37695 17.8545L9.60547 16.7617V14.2988L4.67578 16.459C3.93149 16.8664 3.02649 16.3292 3.02637 15.4541V14.0557C3.02656 13.5099 3.36209 13.0813 3.5752 12.8682L3.63184 12.8164C3.65144 12.8005 3.67221 12.7854 3.69336 12.7715L9.60547 8.88184V5.4209C9.60559 4.10223 10.6813 3.02646 12 3.02637Z"),
            )
        }.build()
        return _il_account_airplane!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountAirplanePreview() {
    Icon(
        imageVector = Icons.il_account_airplane,
        contentDescription = null,
    )
}