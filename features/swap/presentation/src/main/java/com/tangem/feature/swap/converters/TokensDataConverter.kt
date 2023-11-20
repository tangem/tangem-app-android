package com.tangem.feature.swap.converters

import com.tangem.common.Provider
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.CryptoCurrencySwapInfo
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelectState
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
            availableTokens = value.available.map { tokenWithBalanceToTokenToSelect(it) }
                .toMutableList()
                .apply {
                    this.add(0, availableTitle)
                }
                .toImmutableList(),
            unavailableTokens = value.unavailable.map { tokenWithBalanceToTokenToSelect(it) }
                .toMutableList()
                .apply {
                    this.add(0, unavailableTitle)
                }
                .toImmutableList(),
            onSearchEntered = onSearchEntered,
            onTokenSelected = onTokenSelected,
        )
    }

    private fun tokenWithBalanceToTokenToSelect(cryptoCurrencySwapInfo: CryptoCurrencySwapInfo): TokenToSelectState {
        val cryptoCurrencyStatus = cryptoCurrencySwapInfo.currencyStatus
        return TokenToSelectState.TokenToSelect(
            id = cryptoCurrencyStatus.currency.id.value,
            name = cryptoCurrencyStatus.currency.name,
            symbol = cryptoCurrencyStatus.currency.symbol,
            tokenIcon = TokenIconState.CoinIcon(
                url = cryptoCurrencyStatus.currency.iconUrl,
                fallbackResId = cryptoCurrencyStatus.currency.networkIconResId,
                isGrayscale = false,
                showCustomBadge = cryptoCurrencyStatus.currency.isCustom,
            ),
            addedTokenBalanceData = TokenBalanceData(
                amount = formatCryptoAmount(cryptoCurrencyStatus),
                amountEquivalent = formatFiatAmount(cryptoCurrencyStatus, appCurrencyProvider.invoke()),
                isBalanceHidden = isBalanceHiddenProvider.invoke(),
            ),
        )
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