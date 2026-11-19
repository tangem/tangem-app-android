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

private var _il_account_rocket: ImageVector? = null

val Icons.il_account_rocket: ImageVector
    get() {
        if (_il_account_rocket != null) return _il_account_rocket!!
        _il_account_rocket = ImageVector.Builder(
            name = "il_account_rocket",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.753 27.032C18.6398 26.8988 19.5377 27.1948 20.1718 27.8289C20.8057 28.463 21.1008 29.3601 20.9676 30.2468C20.7832 31.4739 19.8199 32.4374 18.5928 32.6218L16.1487 32.9889C15.8352 33.0359 15.5175 32.9318 15.2932 32.7077C15.069 32.4834 14.964 32.1649 15.011 31.8513L15.3782 29.407C15.5625 28.18 16.5262 27.2166 17.753 27.032Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M31.3038 15.0098C32.2562 15.1064 32.9999 15.9107 33 16.8886C33 20.0014 31.585 22.9458 29.1546 24.8904L28.555 25.3699V28.7839C28.5549 29.4992 28.1509 30.1535 27.5111 30.4734L25.3638 31.5466C24.8742 31.7913 24.3023 31.8117 23.7965 31.6022C23.2909 31.3927 22.9004 30.9741 22.7273 30.4548L22.198 28.866C22.0713 28.7889 21.9507 28.6977 21.8416 28.5886L19.4121 26.158C19.3029 26.0488 19.2109 25.9284 19.1338 25.8016L17.546 25.2723C17.0266 25.0991 16.6072 24.7088 16.3977 24.203C16.1883 23.6972 16.2085 23.1252 16.4533 22.6356L17.5275 20.4882L17.5909 20.371C17.929 19.7995 18.5461 19.4443 19.2168 19.4443H22.6316L23.1101 18.8456C25.0546 16.4151 27.9989 15 31.1115 15L31.3038 15.0098ZM24.3571 28.7283C24.3316 28.7486 24.3041 28.7661 24.278 28.7849L24.5836 29.6999L26.5551 28.7146V26.9695L24.3571 28.7283ZM18.2989 23.4149L19.2148 23.7205C19.2336 23.6944 19.2522 23.6678 19.2725 23.6424L21.0311 21.4442H19.2851L18.2989 23.4149Z"),
            )
        }.build()
        return _il_account_rocket!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountRocketPreview() {
    Icon(
        imageVector = Icons.il_account_rocket,
        contentDescription = null,
    )
}