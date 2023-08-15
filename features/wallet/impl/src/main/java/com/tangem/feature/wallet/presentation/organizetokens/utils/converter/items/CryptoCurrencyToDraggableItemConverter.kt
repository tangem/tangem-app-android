package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items

import androidx.annotation.DrawableRes
import com.tangem.common.Provider
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupHeaderId
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getTokenItemId
import com.tangem.utils.converter.Converter

internal class CryptoCurrencyToDraggableItemConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<CryptoCurrencyStatus, DraggableItem.Token> {

    private val CryptoCurrency.networkIconResId: Int?
        @DrawableRes get() {
            // TODO: [REDACTED_JIRA]
            return if (this is CryptoCurrency.Coin) null else R.drawable.img_eth_22
        }

    private val CryptoCurrency.tokenIconResId: Int
        @DrawableRes get() {
            // TODO: [REDACTED_JIRA]
            return R.drawable.img_eth_22
        }

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
            groupId = getGroupHeaderId(currencyStatus.currency.networkId),
        )
    }

    private fun createTokenItemState(
        currencyStatus: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
    ): TokenItemState.Draggable {
        val currency = currencyStatus.currency

        return TokenItemState.Draggable(
            id = getTokenItemId(currency.id),
            tokenIconUrl = currency.iconUrl,
            tokenIconResId = currency.tokenIconResId,
            networkIconResId = currency.networkIconResId,
            name = currency.name,
            fiatAmount = getFormattedFiatAmount(currencyStatus, appCurrency),
        )
    }

    private fun getFormattedFiatAmount(currency: CryptoCurrencyStatus, appCurrency: AppCurrency): String {
        val fiatAmount = currency.value.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
    }
}