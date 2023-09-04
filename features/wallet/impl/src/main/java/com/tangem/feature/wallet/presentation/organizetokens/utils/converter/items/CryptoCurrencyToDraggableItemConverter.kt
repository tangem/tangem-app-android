package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items

import com.tangem.common.Provider
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.utils.CryptoCurrencyToIconStateConverter
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupHeaderId
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getTokenItemId
import com.tangem.utils.converter.Converter

internal class CryptoCurrencyToDraggableItemConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<CryptoCurrencyStatus, DraggableItem.Token> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: CryptoCurrencyStatus): DraggableItem.Token {
        return createDraggableToken(value, appCurrencyProvider())
    }

    override fun convertList(input: Collection<CryptoCurrencyStatus>): List<DraggableItem.Token> {
        val appCurrency = appCurrencyProvider()

        return input.map { createDraggableToken(it, appCurrency) }
    }

    private fun createDraggableToken(
        currencyStatus: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
    ): DraggableItem.Token {
        return DraggableItem.Token(
            tokenItemState = createTokenItemState(currencyStatus, appCurrency),
            groupId = getGroupHeaderId(currencyStatus.currency.network.id),
        )
    }

    private fun createTokenItemState(
        currencyStatus: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
    ): TokenItemState.Draggable {
        val currency = currencyStatus.currency

        return TokenItemState.Draggable(
            id = getTokenItemId(currency.id),
            icon = iconStateConverter.convert(currency),
            name = currency.name,
            fiatAmount = getFormattedFiatAmount(currencyStatus, appCurrency),
        )
    }

    private fun getFormattedFiatAmount(currency: CryptoCurrencyStatus, appCurrency: AppCurrency): String {
        val fiatAmount = currency.value.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
    }
}