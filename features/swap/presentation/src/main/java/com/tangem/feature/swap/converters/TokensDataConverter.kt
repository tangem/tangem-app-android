package com.tangem.feature.swap.converters

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.CryptoCurrencySwapInfo
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelectState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

class TokensDataConverter(
    private val onSearchEntered: (String) -> Unit,
    private val onTokenSelected: (String) -> Unit,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<CurrenciesGroup, SwapSelectTokenStateHolder> {

    override fun convert(value: CurrenciesGroup): SwapSelectTokenStateHolder {
        val availableTitle = TokenToSelectState.Title(stringReference("My tokens")) // todo replace with resource
        val unavailableTitle = TokenToSelectState.Title(stringReference("My tokens")) // todo replace with resource
        return SwapSelectTokenStateHolder(
            availableTokens = value.available.map { tokenWithBalanceToTokenToSelect(it, true) }
                .toMutableList()
                .apply {
                    if (this.isNotEmpty()) {
                        this.add(0, availableTitle)
                    }
                }
                .toImmutableList(),
            unavailableTokens = value.unavailable.map { tokenWithBalanceToTokenToSelect(it, false) }
                .toMutableList()
                .apply {
                    if (this.isNotEmpty()) {
                        this.add(0, unavailableTitle)
                    }
                }
                .toImmutableList(),
            onSearchEntered = onSearchEntered,
            onTokenSelected = onTokenSelected,
        )
    }

    private fun tokenWithBalanceToTokenToSelect(
        cryptoCurrencySwapInfo: CryptoCurrencySwapInfo,
        isAvailable: Boolean,
    ): TokenToSelectState {
        val cryptoCurrencyStatus = cryptoCurrencySwapInfo.currencyStatus
        return TokenToSelectState.TokenToSelect(
            id = cryptoCurrencyStatus.currency.id.value,
            name = cryptoCurrencyStatus.currency.name,
            symbol = cryptoCurrencyStatus.currency.symbol,
            available = isAvailable,
            tokenIcon = convertIcon(cryptoCurrencyStatus.currency, isAvailable),
            addedTokenBalanceData = TokenBalanceData(
                amount = formatCryptoAmount(cryptoCurrencyStatus),
                amountEquivalent = formatFiatAmount(cryptoCurrencyStatus, appCurrencyProvider.invoke()),
                isBalanceHidden = isBalanceHiddenProvider.invoke(),
            ),
        )
    }

    private fun convertIcon(currency: CryptoCurrency, isAvailable: Boolean): TokenIconState {
        return when (currency) {
            is CryptoCurrency.Coin -> {
                TokenIconState.CoinIcon(
                    url = currency.iconUrl,
                    fallbackResId = currency.networkIconResId,
                    isGrayscale = !isAvailable,
                    showCustomBadge = currency.isCustom,
                )
            }
            is CryptoCurrency.Token -> {
                val isGrayscale = currency.network.isTestnet
                val background = currency.tryGetBackgroundForTokenIcon(isGrayscale)
                val tint = getTintForTokenIcon(background)
                TokenIconState.TokenIcon(
                    url = currency.iconUrl,
                    isGrayscale = !isAvailable,
                    showCustomBadge = currency.isCustom,
                    networkBadgeIconResId = currency.networkIconResId,
                    fallbackTint = tint,
                    fallbackBackground = background,
                )
            }
        }
    }

    private fun formatCryptoAmount(cryptoCurrencyStatus: CryptoCurrencyStatus): String {
        return BigDecimalFormatter.formatCryptoAmount(
            cryptoCurrencyStatus.value.amount,
            cryptoCurrencyStatus.currency.symbol,
            cryptoCurrencyStatus.currency.decimals,
        )
    }

    private fun formatFiatAmount(cryptoCurrencyStatus: CryptoCurrencyStatus, appCurrency: AppCurrency): String {
        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = cryptoCurrencyStatus.value.fiatAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }
}