package com.tangem.core.ui.extensions

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.toColorInt
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network

private const val LIGHT_LUMINANCE = 0.5f
private const val COLOR_HEX_START_INDEX = 2
private const val COLOR_HEX_END_INDEX = 7

/**
 * Retrieves the resource ID for the network of a [CryptoCurrency].
 *
 * @return Drawable resource ID for the network.
 */
@get:DrawableRes
val CryptoCurrency.networkIconResId: Int
    get() = network.iconResId

/**
 * Retrieves the resource ID.
 *
 * @return Drawable resource ID for the network.
 */
val Network.iconResId: Int
    get() = getActiveIconRes(id.value)

/**
 * Tries to extract a background color from the contract address of a token.
 *
 * @param fallbackColor The color to use as a fallback.
 * @return The extracted background color or the fallback color if extraction fails or if it is a test network token.
 */
fun CryptoCurrency.Token.tryGetBackgroundForTokenIcon(
    isGrayscale: Boolean,
    fallbackColor: Color = TangemColorPalette.Black,
): Color {
    if (isGrayscale) return TangemColorPalette.Dark2

    return tryGetBackgroundForTokenIcon(contractAddress = contractAddress, fallbackColor = fallbackColor)
}

/**
 * Tries to extract a background color from the contract address of a token.
 *
 * @param fallbackColor The color to use as a fallback.
 * @return The extracted background color or the fallback color if extraction fails or if it is a test network token.
 */
fun tryGetBackgroundForTokenIcon(contractAddress: String, fallbackColor: Color = TangemColorPalette.Black): Color {
    return try {
        val colorHex = "#" + contractAddress.substring(range = COLOR_HEX_START_INDEX..COLOR_HEX_END_INDEX)
        Color(colorHex.toColorInt())
    } catch (exception: Exception) {
        fallbackColor
    }
}

/**
 * Determines the tint color to be used for a token icon based on its background color.
 * If the icon's background color is light, a dark tint is chosen; otherwise, a light tint is chosen.
 *
 * @param iconBackground The background color of the custom token icon.
 * @return The tint color to be used for the icon.
 */
fun getTintForTokenIcon(iconBackground: Color): Color {
    return if (iconBackground.luminance() > LIGHT_LUMINANCE) TangemColorPalette.Black else TangemColorPalette.White
}