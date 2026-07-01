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

private var _ic_heart_32: ImageVector? = null

val Icons.ic_heart_32: ImageVector
    get() {
        if (_ic_heart_32 != null) return _ic_heart_32!!
        _ic_heart_32 = ImageVector.Builder(
            name = "ic_heart_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.834 4.5C25.8256 4.50032 29.0046 9.05209 29.0049 13.1299C29.0048 15.2713 28.1491 17.2402 26.9756 18.9355C25.8006 20.633 24.2568 22.1319 22.748 23.3643C21.2341 24.6008 19.7184 25.5981 18.5693 26.2871C17.9941 26.632 17.5046 26.9034 17.1465 27.0908C16.9686 27.1839 16.8166 27.2594 16.6992 27.3145C16.6423 27.3411 16.5824 27.3688 16.5264 27.3916C16.4999 27.4024 16.4584 27.418 16.4111 27.4336C16.3879 27.4412 16.3481 27.4543 16.2998 27.4658C16.2696 27.4731 16.1523 27.5009 16.002 27.501C15.8526 27.5009 15.7363 27.4733 15.7051 27.4658C15.657 27.4543 15.6171 27.4413 15.5938 27.4336C15.5467 27.4181 15.5051 27.4024 15.4785 27.3916C15.4226 27.3688 15.3625 27.3411 15.3057 27.3145C15.1884 27.2595 15.0361 27.1838 14.8584 27.0908C14.5003 26.9035 14.0107 26.6319 13.4355 26.2871C12.2866 25.5982 10.7707 24.6007 9.25684 23.3643C7.74815 22.132 6.20426 20.6328 5.0293 18.9355C3.8558 17.2402 3.00012 15.2713 3 13.1299C3.00024 9.05194 6.17897 4.5 11.1709 4.5C13.306 4.50013 14.8942 5.29749 16.002 6.20703C17.1098 5.29726 18.6983 4.5 20.834 4.5ZM20.834 7C18.9179 7 17.6812 7.92499 16.9414 8.7666C16.7042 9.03641 16.3612 9.19127 16.002 9.19141C15.6429 9.19128 15.3007 9.03609 15.0635 8.7666C14.3238 7.92506 13.0866 7.00017 11.1709 7C7.88427 7 5.50024 10.084 5.5 13.1299C5.50012 14.595 6.08771 16.0719 7.08496 17.5127C8.08079 18.9511 9.43611 20.282 10.8389 21.4277C12.2366 22.5693 13.6468 23.4997 14.7207 24.1436C15.248 24.4597 15.6898 24.7035 16.002 24.8672C16.3142 24.7034 16.7564 24.46 17.2842 24.1436C18.3582 23.4996 19.7682 22.5694 21.166 21.4277C22.5689 20.2819 23.9241 18.9513 24.9199 17.5127C25.9172 16.0719 26.5048 14.595 26.5049 13.1299C26.5046 10.0842 24.1203 7.00033 20.834 7Z"),
            )
        }.build()
        return _ic_heart_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart32Preview() {
    Icon(
        imageVector = Icons.ic_heart_32,
        contentDescription = null,
    )
}