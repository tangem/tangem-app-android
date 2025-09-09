package com.tangem.common.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.account.AccountCharIcon
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.AccountResIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.CryptoPortfolioIcon.Color

/**
 * Displays a portfolio account icon that can either show:
 * - a single character (derived from the [name]) using [AccountCharIcon], or
 * - a predefined vector resource (from [icon]) using [AccountResIcon].
 *
 * The background color is taken from [CryptoPortfolioIconUM.color],
 * and the icon size, text style, and container shape are adapted based on the given [size].
 *
 * @param name A [TextReference] used to resolve and display the first letter
 *             when [icon.value] is set to [CryptoPortfolioIcon.Icon.Letter].
 * @param icon The account icon definition, which can be a letter or a drawable resource.
 * @param size The icon size, defined by [AccountIconSize].
 *
 * @see AccountCharIcon
 * @see AccountResIcon
 * @see AccountIconSize
 */
@Composable
fun AccountIcon(
    name: TextReference,
    icon: CryptoPortfolioIconUM,
    size: AccountIconSize,
    modifier: Modifier = Modifier,
) {
    val letter = name.resolveReference().firstOrNull()
    when {
        icon.value == CryptoPortfolioIcon.Icon.Letter -> AccountCharIcon(
            char = letter ?: 'N',
            color = icon.color.getUiColor(),
            size = size,
            modifier = modifier,
        )
        else -> AccountResIcon(
            resId = icon.value.getResId(),
            color = icon.color.getUiColor(),
            size = size,
            modifier = modifier,
        )
    }
}

object AccountIconPreviewData {

    fun randomAccountIcon(letter: Boolean = false) = CryptoPortfolioIconUM(
        value = if (letter) CryptoPortfolioIcon.Icon.Letter else CryptoPortfolioIcon.Icon.entries.random(),
        color = Color.entries.random(),
    )
}