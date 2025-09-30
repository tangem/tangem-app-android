package com.tangem.features.managetokens.utils.mapper

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.item.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.utils.ui.getIconRes
import kotlinx.collections.immutable.toImmutableList

internal fun ManagedCryptoCurrency.toUiModel(
    isEditable: Boolean,
    onTokenClick: (ManagedCryptoCurrency.Token) -> Unit,
    onRemoveCustomCurrencyClick: (ManagedCryptoCurrency.Custom) -> Unit,
    isCollapsed: Boolean,
): CurrencyItemUM = when (this) {
    is ManagedCryptoCurrency.Custom -> toUiModel(onRemoveCustomCurrencyClick)
    is ManagedCryptoCurrency.Token -> toUiModel(isEditable, onTokenClick, isCollapsed)
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
                shouldShowCustomBadge = true,
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
                shouldShowCustomBadge = true,
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
    isCollapsed: Boolean,
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
            shouldShowCustomBadge = false,
            fallbackTint = getTintForTokenIcon(background),
            fallbackBackground = background,
        ),
        networks = if (isCollapsed) {
            NetworksUM.Collapsed
        } else {
            NetworksUM.Expanded(
                availableNetworks.map {
                    it.toCurrencyNetworkModel(
                        isSelected = it.network in addedIn,
                        isEditable = false,
                        onSelectedStateChange = { _, _ -> },
                        onLongTap = { _ -> },
                    )
                }.toImmutableList(),
            )
        },
        onExpandClick = {
            onTokenClick(this)
        },
    )
}