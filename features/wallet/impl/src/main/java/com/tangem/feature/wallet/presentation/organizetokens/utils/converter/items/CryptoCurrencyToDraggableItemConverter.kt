package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items

import androidx.annotation.DrawableRes
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupHeaderId
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getTokenItemId
import com.tangem.utils.converter.Converter

internal class CryptoCurrencyToDraggableItemConverter(
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
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
        return DraggableItem.Token(
            tokenItemState = createToTokenItemState(value),
            groupId = getGroupHeaderId(value.currency.networkId),
        )
    }

    private fun createToTokenItemState(currencyStatus: CryptoCurrencyStatus): TokenItemState.Draggable {
        val currency = currencyStatus.currency

        return TokenItemState.Draggable(
            id = getTokenItemId(currency.id),
            tokenIconUrl = currency.iconUrl,
            tokenIconResId = currency.tokenIconResId,
            networkIconResId = currency.networkIconResId,
            name = currency.name,
            fiatAmount = getFormattedFiatAmount(currencyStatus),
        )
    }

    private fun getFormattedFiatAmount(currency: CryptoCurrencyStatus): String {
        val fiatAmount = currency.value.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, fiatCurrencyCode, fiatCurrencySymbol)
    }
}