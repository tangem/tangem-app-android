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

private var _il_account_magic_ball: ImageVector? = null

val Icons.il_account_magic_ball: ImageVector
    get() {
        if (_il_account_magic_ball != null) return _il_account_magic_ball!!
        _il_account_magic_ball = ImageVector.Builder(
            name = "il_account_magic_ball",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.1451 17.3511C21.4262 14.2162 26.5738 14.2164 29.855 17.3511C32.8717 20.2336 33.336 24.8358 31.1034 28.246C31.1322 28.2871 31.1602 28.3294 31.1859 28.373L31.25 28.4902L31.813 29.6243C32.1722 30.3474 32.1346 31.2066 31.712 31.8943C31.2895 32.5818 30.5427 32.9998 29.7395 33H18.2606C17.4574 33 16.7108 32.5817 16.2881 31.8943C15.8656 31.2066 15.8269 30.3474 16.1862 29.6243L16.7502 28.4902L16.8133 28.373C16.8391 28.3291 16.8667 28.2864 16.8958 28.2451C14.6641 24.8349 15.1287 20.2332 18.1451 17.3511ZM17.9645 30.519C17.9135 30.6219 17.919 30.7444 17.9791 30.8423C18.0394 30.9399 18.146 30.9996 18.2606 30.9996H29.7395C29.854 30.9994 29.9609 30.94 30.021 30.8423C30.0811 30.7446 30.0864 30.6218 30.0356 30.519L29.5027 29.4465H18.4975L17.9645 30.519ZM25.7624 18.0466L25.6605 18.0514C25.1592 18.1026 24.7674 18.5289 24.7674 19.0468C24.7672 19.7294 24.2172 20.2834 23.5375 20.2834C23.0392 20.2835 22.6279 20.6527 22.5561 21.1341C22.5487 21.1833 22.5435 21.2334 22.5435 21.2846C22.5435 21.8369 22.9886 22.2846 23.5375 22.2848C24.2172 22.2848 24.7672 22.8387 24.7674 23.5214C24.7674 23.9179 24.9979 24.2592 25.3305 24.421C25.3638 24.4372 25.3981 24.4515 25.4333 24.464C25.5362 24.5003 25.6463 24.5206 25.7615 24.5206L25.7624 24.5216C26.3114 24.5216 26.7565 24.0738 26.7565 23.5214C26.7567 22.8389 27.307 22.2851 27.9864 22.2848C28.5354 22.2848 28.9804 21.837 28.9804 21.2846C28.9804 21.2496 28.979 21.2151 28.9755 21.181C28.9245 20.6767 28.501 20.2834 27.9864 20.2834C27.307 20.2831 26.7567 19.7292 26.7565 19.0468C26.7565 18.4944 26.3114 18.0466 25.7624 18.0466ZM22.2775 23.3065C22.2775 22.7541 21.8325 22.3063 21.2835 22.3063C20.7346 22.3064 20.2894 22.7542 20.2894 23.3065V23.5849H20.0108C19.462 23.585 19.0168 24.0328 19.0168 24.5851C19.0168 25.1374 19.462 25.5852 20.0108 25.5853H20.2894V25.8637C20.2894 26.416 20.7346 26.8638 21.2835 26.8639C21.8325 26.8639 22.2775 26.4161 22.2775 25.8637V25.5853H22.5532C23.1022 25.5853 23.5472 25.1375 23.5472 24.5851C23.5472 24.0327 23.1022 23.5849 22.5532 23.5849H22.2775V23.3065Z"),
            )
        }.build()
        return _il_account_magic_ball!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountMagicBallPreview() {
    Icon(
        imageVector = Icons.il_account_magic_ball,
        contentDescription = null,
    )
}