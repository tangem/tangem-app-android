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
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.1451 5.3511C9.42618 2.2162 14.5738 2.21641 17.855 5.3511C20.8717 8.23363 21.336 12.8358 19.1034 16.246C19.1322 16.2871 19.1602 16.3294 19.1859 16.373L19.25 16.4902L19.813 17.6243C20.1722 18.3474 20.1346 19.2066 19.712 19.8943C19.2895 20.5818 18.5427 20.9998 17.7395 21H6.26062C5.45736 21 4.71076 20.5817 4.28809 19.8943C3.86557 19.2066 3.82692 18.3474 4.18617 17.6243L4.75016 16.4902L4.81326 16.373C4.8391 16.3291 4.86675 16.2864 4.89577 16.2451C2.66406 12.8349 3.12871 8.23324 6.1451 5.3511ZM5.96454 18.519C5.9135 18.6219 5.91897 18.7444 5.97911 18.8423C6.03938 18.9399 6.14604 18.9996 6.26062 18.9996H17.7395C17.854 18.9994 17.9609 18.94 18.021 18.8423C18.0811 18.7446 18.0864 18.6218 18.0356 18.519L17.5027 17.4465H6.49748L5.96454 18.519ZM13.7624 6.04656L13.6605 6.05144C13.1592 6.10263 12.7674 6.52887 12.7674 7.04678C12.7672 7.72942 12.2172 8.28337 11.5375 8.28337C11.0392 8.2835 10.6279 8.65272 10.5561 9.13414C10.5487 9.18326 10.5435 9.23338 10.5435 9.28457C10.5435 9.83689 10.9886 10.2846 11.5375 10.2848C12.2172 10.2848 12.7672 10.8387 12.7674 11.5214C12.7674 11.9179 12.9979 12.2592 13.3305 12.421C13.3638 12.4372 13.3981 12.4515 13.4333 12.464C13.5362 12.5003 13.6463 12.5206 13.7615 12.5206L13.7624 12.5216C14.3114 12.5216 14.7565 12.0738 14.7565 11.5214C14.7567 10.8389 15.307 10.2851 15.9864 10.2848C16.5354 10.2848 16.9804 9.83697 16.9804 9.28457C16.9804 9.24961 16.979 9.21506 16.9755 9.18103C16.9245 8.67674 16.501 8.28337 15.9864 8.28337C15.307 8.28305 14.7567 7.72922 14.7565 7.04678C14.7565 6.49437 14.3114 6.04656 13.7624 6.04656ZM10.2775 11.3065C10.2775 10.7541 9.83246 10.3063 9.28347 10.3063C8.7346 10.3064 8.28945 10.7542 8.28945 11.3065V11.5849H8.01085C7.46198 11.585 7.01682 12.0328 7.01682 12.5851C7.01682 13.1374 7.46198 13.5852 8.01085 13.5853H8.28945V13.8637C8.28945 14.416 8.7346 14.8638 9.28347 14.8639C9.83246 14.8639 10.2775 14.4161 10.2775 13.8637V13.5853H10.5532C11.1022 13.5853 11.5472 13.1375 11.5472 12.5851C11.5472 12.0327 11.1022 11.5849 10.5532 11.5849H10.2775V11.3065Z"),
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