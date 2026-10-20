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

private var _ic_bone_24: ImageVector? = null

val Icons.ic_bone_24: ImageVector
    get() {
        if (_ic_bone_24 != null) return _ic_bone_24!!
        _ic_bone_24 = ImageVector.Builder(
            name = "ic_bone_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.0635 6.03125C11.0635 4.08153 12.6438 2.5 14.5938 2.5C16.4905 2.50025 18.0362 3.99614 18.1201 5.87207C19.0022 5.90957 19.8402 6.27709 20.4668 6.9043C21.1286 7.56678 21.5003 8.46498 21.5 9.40137L21.4951 9.58301C21.4008 11.4485 19.8588 12.9326 17.9697 12.9326H16.002L15.9629 12.9404C15.9506 12.9455 15.9393 12.9532 15.9297 12.9629L12.9629 15.9307C12.9444 15.9491 12.9337 15.9747 12.9336 16.001V17.9688C12.9336 19.9167 11.3563 21.4975 9.4082 21.5L9.40625 20.5V21.5C7.50652 21.4997 5.95836 19.9992 5.87891 18.1191C4.99715 18.0814 4.15954 17.7149 3.5332 17.0879C2.87121 16.4252 2.49943 15.5266 2.5 14.5898C2.5004 12.6405 4.08058 11.0596 6.03027 11.0596H8C8.02648 11.0594 8.05231 11.049 8.07129 11.0303L11.0342 8.06152L11.0557 8.0293C11.0607 8.01737 11.0634 8.00444 11.0635 7.99121V6.03125ZM13.0635 7.99805C13.0615 8.55165 12.8404 9.08179 12.4492 9.47363L12.4502 9.47461L9.4834 12.4463C9.09192 12.8365 8.56257 13.057 8.00977 13.0596H6.03027C5.18576 13.0596 4.5 13.7448 4.5 14.5908L4.50781 14.7422C4.54234 15.0931 4.69703 15.4233 4.94824 15.6748C5.23519 15.9619 5.62463 16.123 6.03027 16.123H6.875C7.42724 16.123 7.87492 16.5708 7.875 17.123V17.9678C7.8752 18.814 8.5607 19.4982 9.40527 19.499C10.2491 19.498 10.9336 18.814 10.9336 17.9688V15.9941C10.9355 15.4404 11.1566 14.9094 11.5479 14.5176L14.5117 11.5518C14.9051 11.1564 15.4401 10.9337 15.998 10.9326H17.9697C18.8142 10.9326 19.5 10.2474 19.5 9.40137L19.4922 9.25C19.4576 8.89912 19.303 8.56885 19.0518 8.31738C18.7648 8.03029 18.3754 7.86914 17.9697 7.86914H17.125C16.5727 7.86914 16.125 7.4214 16.125 6.86914V6.03223C16.1248 5.1857 15.4388 4.50026 14.5938 4.5C13.7492 4.5 13.0635 5.18524 13.0635 6.03125V7.99805Z"),
            )
        }.build()
        return _ic_bone_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBone24Preview() {
    Icon(
        imageVector = Icons.ic_bone_24,
        contentDescription = null,
    )
}