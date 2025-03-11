package com.tangem.core.ui.components.icons.identicon

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

private const val GAP_WIDTH = 1f
private const val ALPHA = 0.9f

/**
 * Ident icon
 *
 * @param address to generate ident icon from
 * @param modifier to apply to ident icon
 */
@Suppress("MagicNumber")
@Composable
fun IdentIcon(address: String, modifier: Modifier = Modifier) {
    if (address.isBlank()) {
        Box(modifier = modifier)
    } else {
        val blockies = Blockies(address.toLowerCase(Locale.current))
        Canvas(
            modifier = modifier
                .then(
                    if (LocalIsInDarkTheme.current) {
                        Modifier.alpha(ALPHA)
                    } else {
                        Modifier
                    },
                ),
        ) {
            val cellWidth = size.width / Blockies.SIZE
            blockies.data.forEachIndexed { index, item ->
                val y = index / Blockies.SIZE
                val x = index % Blockies.SIZE
                val colorInt = when (item) {
                    1f -> blockies.primaryColor
                    2f -> blockies.spotColor
                    else -> blockies.backgroundColor
                }
                drawRect(
                    color = Color(colorInt),
                    topLeft = Offset(x * cellWidth, y * cellWidth),
                    size = Size(cellWidth + GAP_WIDTH, cellWidth + GAP_WIDTH),
                )
            }
        }
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IdentIconPreview() {
    TangemThemePreview {
        IdentIcon(
            address = "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359",
            modifier = Modifier
                .size(TangemTheme.dimens.size40),
        )
    }
}
//endregion