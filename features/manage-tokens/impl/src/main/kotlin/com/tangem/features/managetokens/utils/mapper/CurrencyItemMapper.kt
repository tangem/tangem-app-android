package com.tangem.features.managetokens.utils.mapper

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.item.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.utils.ui.getIconRes

internal fun ManagedCryptoCurrency.toUiModel(
    isEditable: Boolean,
    onTokenClick: (ManagedCryptoCurrency.Token) -> Unit,
    onRemoveCustomCurrencyClick: (ManagedCryptoCurrency.Custom) -> Unit,
): CurrencyItemUM = when (this) {
    is ManagedCryptoCurrency.Custom -> toUiModel(onRemoveCustomCurrencyClick)
    is ManagedCryptoCurrency.Token -> toUiModel(isEditable, onTokenClick)
}

private fun ManagedCryptoCurrency.Custom.toUiModel(
    onRemoveCustomCurrency: (ManagedCryptoCurrency.Custom) -> Unit,
): CurrencyItemUM = CurrencyItemUM.Custom(
    id = id,
    name = name,
    symbol = symbol,
    icon = when (this) {
        is ManagedCryptoCurrency.Custom.Coin -> {
            CurrencyIconState.CoinIcon(
                url = iconUrl,
                fallbackResId = network.id.getIconRes(isColored = true),
                isGrayscale = false,
                showCustomBadge = true,
            )
        }
        is ManagedCryptoCurrency.Custom.Token -> {
            val background = tryGetBackgroundForTokenIcon(contractAddress)

            CurrencyIconState.TokenIcon(
                url = iconUrl,
                fallbackBackground = background,
                fallbackTint = getTintForTokenIcon(background),
                topBadgeIconResId = network.id.getIconRes(isColored = true),
                isGrayscale = false,
                showCustomBadge = true,
            )
        }
    },
    onRemoveClick = {
        onRemoveCustomCurrency(this)
    },
)

private fun ManagedCryptoCurrency.Token.toUiModel(
    isEditable: Boolean,
    onTokenClick: (ManagedCryptoCurrency.Token) -> Unit,
): CurrencyItemUM {
    val background = TangemColorPalette.Black

    return CurrencyItemUM.Basic(
        id = id,
        name = name,
        symbol = symbol,
        icon = CurrencyIconState.TokenIcon(
            url = iconUrl,
            topBadgeIconResId = null,
            isGrayscale = if (isEditable) !isAdded else false,
            showCustomBadge = false,
            fallbackTint = getTintForTokenIcon(background),
            fallbackBackground = background,
        ),
        networks = NetworksUM.Collapsed,
        onExpandClick = {
            onTokenClick(this)
        },
    )
}