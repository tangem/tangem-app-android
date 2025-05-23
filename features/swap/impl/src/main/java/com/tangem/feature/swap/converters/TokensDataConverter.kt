package com.tangem.feature.swap.converters

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.CryptoCurrencySwapInfo
import com.tangem.feature.swap.models.CurrenciesGroupWithFromCurrency
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelectState
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

class TokensDataConverter(
    private val onSearchEntered: (String) -> Unit,
    private val onTokenSelected: (String) -> Unit,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<CurrenciesGroupWithFromCurrency, SwapSelectTokenStateHolder> {

    override fun convert(value: CurrenciesGroupWithFromCurrency): SwapSelectTokenStateHolder {
        val group = value.group
        val availableTitle = TokenToSelectState.Title(
            resourceReference(R.string.exchange_tokens_available_tokens_header),
        )
        val unavailableTitle = TokenToSelectState.Title(
            resourceReference(
                R.string.exchange_tokens_unavailable_tokens_header,
                wrappedList(value.fromCurrency.name),
            ),
        )
        return SwapSelectTokenStateHolder(
            availableTokens = group.available.map { tokenWithBalanceToTokenToSelect(it, true) }
                .toMutableList()
                .apply {
                    if (this.isNotEmpty()) {
                        this.add(0, availableTitle)
                    }
                }
                .toImmutableList(),
            unavailableTokens = group.unavailable.map { tokenWithBalanceToTokenToSelect(it, false) }
                .toMutableList()
                .apply {
                    if (this.isNotEmpty()) {
                        this.add(0, unavailableTitle)
                    }
                }
                .toImmutableList(),
            onSearchEntered = onSearchEntered,
            onTokenSelected = onTokenSelected,
            afterSearch = group.afterSearch,
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

    private fun convertIcon(currency: CryptoCurrency, isAvailable: Boolean): CurrencyIconState {
        return when (currency) {
            is CryptoCurrency.Coin -> {
                CurrencyIconState.CoinIcon(
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
                CurrencyIconState.TokenIcon(
                    url = currency.iconUrl,
                    isGrayscale = !isAvailable,
                    showCustomBadge = currency.isCustom,
                    topBadgeIconResId = currency.networkIconResId,
                    fallbackTint = tint,
                    fallbackBackground = background,
                )
            }
        }
    }

    private fun formatCryptoAmount(cryptoCurrencyStatus: CryptoCurrencyStatus): String {
        return cryptoCurrencyStatus.value.amount.format {
            crypto(cryptoCurrencyStatus.currency)
        }
    }

    private fun formatFiatAmount(cryptoCurrencyStatus: CryptoCurrencyStatus, appCurrency: AppCurrency): String {
        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = cryptoCurrencyStatus.value.fiatAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }
}