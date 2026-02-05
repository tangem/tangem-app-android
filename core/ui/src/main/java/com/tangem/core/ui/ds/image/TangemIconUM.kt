package com.tangem.core.ui.ds.image

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.res.TangemTheme

/**
 * Model representing different types of icons that can be displayed in the UI.
 */
@Immutable
sealed interface TangemIconUM {

    /** Icon representing a currency. */
    data class Currency(
        val currencyIconState: CurrencyIconState,
    ) : TangemIconUM

    /** Icon represented by a drawable resource. */
    data class Icon(
        @DrawableRes val iconRes: Int,
        val tintReference: ColorReference2 = ColorReference2 { TangemTheme.colors2.graphic.neutral.primary },
    ) : TangemIconUM

    /** Image represented by a drawable resource. */
    data class Image(
        @DrawableRes val imageRes: Int,
    ) : TangemIconUM

    /** Identicon represented by a text string (e.g., an address). */
    data class Ident(
        val text: String,
    ) : TangemIconUM
}

/**
 * Composable function to display an icon based on the provided [TangemIconUM] type.
 *
 * @param tangemIconUM The [TangemIconUM] instance representing the icon to be displayed.
 * @param modifier The [Modifier] to be applied to the icon.
 */
@Composable
fun TangemIcon(tangemIconUM: TangemIconUM, modifier: Modifier = Modifier) {
    when (tangemIconUM) {
        is TangemIconUM.Currency -> {
            CurrencyIcon(
                state = tangemIconUM.currencyIconState,
                modifier = modifier,
            )
        }
        is TangemIconUM.Icon -> Icon(
            imageVector = ImageVector.vectorResource(tangemIconUM.iconRes),
            contentDescription = null,
            modifier = modifier,
            tint = tangemIconUM.tintReference(),
        )
        is TangemIconUM.Image -> Image(
            imageVector = ImageVector.vectorResource(tangemIconUM.imageRes),
            contentDescription = null,
            modifier = modifier,
        )
        is TangemIconUM.Ident -> IdentIcon(
            address = tangemIconUM.text,
            modifier = modifier,
        )
    }
}