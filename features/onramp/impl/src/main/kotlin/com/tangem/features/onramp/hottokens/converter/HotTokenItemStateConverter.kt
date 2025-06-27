package com.tangem.features.onramp.hottokens.converter

import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.quote.fold
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Token item state converter from [HotCryptoCurrency] to [TokenItemState]
 *
 * @property appCurrency app currency
 * @property onItemClick callback is invoked when item is clicked
 */
internal class HotTokenItemStateConverter(
    private val appCurrency: AppCurrency,
    private val onItemClick: (TokenItemState, HotCryptoCurrency) -> Unit,
) : Converter<HotCryptoCurrency, TokenItemState> {

    override fun convert(value: HotCryptoCurrency): TokenItemState {
        return TokenItemState.Content(
            id = value.cryptoCurrency.id.value,
            iconState = CryptoCurrencyToIconStateConverter().convert(value.cryptoCurrency),
            titleState = TokenItemState.TitleState.Content(text = stringReference(value.cryptoCurrency.name)),
            subtitleState = value.quoteStatus.getCryptoPriceState(appCurrency),
            fiatAmountState = null,
            subtitle2State = null,
            onItemClick = onItemClick.let { onItemClick -> { onItemClick(it, value) } },
            onItemLongClick = null,
        )
    }

    private fun QuoteStatus.getCryptoPriceState(appCurrency: AppCurrency): TokenItemState.SubtitleState {
        return fold(
            onData = {
                TokenItemState.SubtitleState.CryptoPriceContent(
                    price = fiatRate.getFormattedCryptoPrice(appCurrency),
                    priceChangePercent = priceChange.format { percent() },
                    type = priceChange.getPriceChangeType(),
                )
            },
            onEmpty = { TokenItemState.SubtitleState.Unknown },
        )
    }

    private fun BigDecimal.getFormattedCryptoPrice(appCurrency: AppCurrency): String {
        return format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
    }

    private fun BigDecimal.getPriceChangeType(): PriceChangeType {
        return PriceChangeConverter.fromBigDecimal(value = this)
    }
}