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

private var _il_account_shirt: ImageVector? = null

val Icons.il_account_shirt: ImageVector
    get() {
        if (_il_account_shirt != null) return _il_account_shirt!!
        _il_account_shirt = ImageVector.Builder(
            name = "il_account_shirt",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M26.565 15.4128C26.6957 15.4129 26.8261 15.4196 26.9557 15.4322C26.9606 15.4326 26.9654 15.4326 26.9702 15.4332C27.4257 15.479 27.8716 15.602 28.2876 15.7978L29.7714 16.4962C30.8273 16.9933 31.6173 17.9227 31.938 19.0449L32.9401 22.5512C33.0578 22.9633 33.0012 23.406 32.7844 23.7757C32.5675 24.1454 32.2082 24.4105 31.791 24.5089L29.6863 25.0041L29.6872 29.8152C29.6955 30.8912 28.978 31.8382 27.9394 32.1221C27.9312 32.1244 27.9226 32.1269 27.9143 32.1289C25.3333 32.7396 22.6451 32.7396 20.0641 32.1289C20.056 32.127 20.0479 32.1243 20.0399 32.1221C18.9989 31.8374 18.2803 30.8866 18.2921 29.8075V24.9983L16.2096 24.5089C15.7923 24.4106 15.4332 24.1454 15.2162 23.7757C14.9993 23.4059 14.9427 22.9634 15.0605 22.5512L16.0625 19.0449C16.3833 17.9226 17.1732 16.9933 18.2292 16.4962L19.713 15.7978C20.2517 15.5443 20.8402 15.4129 21.4356 15.4128L22.016 16.8985C22.0998 17.078 22.2128 17.2443 22.3555 17.387C22.6776 17.7091 23.1149 17.8899 23.5704 17.89H24.4293C24.8847 17.8899 25.322 17.709 25.6441 17.387C25.7868 17.2443 25.8998 17.078 25.9836 16.8985L26.565 15.4128Z"),
            )
        }.build()
        return _il_account_shirt!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountShirtPreview() {
    Icon(
        imageVector = Icons.il_account_shirt,
        contentDescription = null,
    )
}