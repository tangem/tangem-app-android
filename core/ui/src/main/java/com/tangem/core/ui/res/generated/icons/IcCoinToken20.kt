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

private var _ic_coin_token_20: ImageVector? = null

val Icons.ic_coin_token_20: ImageVector
    get() {
        if (_ic_coin_token_20 != null) return _ic_coin_token_20!!
        _ic_coin_token_20 = ImageVector.Builder(
            name = "ic_coin_token_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.2932 7.29492C7.58599 7.00228 8.06087 7.00243 8.35375 7.29492L10.5286 9.46973C10.8211 9.7626 10.8212 10.2375 10.5286 10.5303L8.35375 12.7051C8.06093 12.9974 7.58595 12.9975 7.2932 12.7051L5.11938 10.5303C4.82659 10.2375 4.82681 9.76265 5.11938 9.46973L7.2932 7.29492ZM6.7102 10L7.82348 11.1143L8.93774 10L7.82348 8.88672L6.7102 10Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1731 4.17578C15.3895 4.17591 17.9973 6.78358 17.9973 10C17.9972 13.2163 15.3894 15.8241 12.1731 15.8242C11.405 15.8242 10.6748 15.6699 10.0071 15.4004C7.90567 16.2486 5.4098 15.8214 3.70629 14.1182C1.43189 11.8438 1.43189 8.15624 3.70629 5.88184C5.40971 4.17852 7.90571 3.75051 10.0071 4.59863C10.6748 4.32932 11.405 4.17581 12.1731 4.17578ZM9.61449 6.06348C8.02077 5.33867 6.0779 5.63143 4.76684 6.94238C3.07822 8.631 3.07822 11.369 4.76684 13.0576C6.07657 14.367 8.01666 14.6594 9.60961 13.9375C9.6536 13.9108 9.7 13.8882 9.74828 13.8711C10.157 13.6679 10.5415 13.3982 10.8821 13.0576C12.5177 11.4218 12.5687 8.80087 11.0354 7.10352L10.8821 6.94238L10.72 6.78906C10.4122 6.51098 10.0732 6.28618 9.71606 6.1123C9.68121 6.09808 9.64693 6.08282 9.61449 6.06348ZM12.1731 5.67578C12.03 5.67579 11.8888 5.68428 11.7493 5.69824C11.8145 5.75787 11.8795 5.8187 11.9426 5.88184L12.1487 6.09863C14.2144 8.3848 14.1459 11.9148 11.9426 14.1182C11.8796 14.1811 11.8143 14.2413 11.7493 14.3008C11.8888 14.3148 12.03 14.3242 12.1731 14.3242C14.561 14.3241 16.4972 12.3879 16.4973 10C16.4973 7.61201 14.561 5.67591 12.1731 5.67578Z"),
            )
        }.build()
        return _ic_coin_token_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCoinToken20Preview() {
    Icon(
        imageVector = Icons.ic_coin_token_20,
        contentDescription = null,
    )
}