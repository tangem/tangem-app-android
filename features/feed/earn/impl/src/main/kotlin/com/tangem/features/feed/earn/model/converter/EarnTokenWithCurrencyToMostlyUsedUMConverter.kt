package com.tangem.features.feed.earn.model.converter

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.features.feed.earn.ui.state.EarnOpportunitiesItemUM
import com.tangem.utils.converter.Converter

internal class EarnTokenWithCurrencyToMostlyUsedUMConverter(
    private val onItemClick: (EarnTokenWithCurrency) -> Unit,
) : Converter<EarnTokenWithCurrency, EarnOpportunitiesItemUM> {

    private val cryptoCurrencyToIconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: EarnTokenWithCurrency): EarnOpportunitiesItemUM {
        return EarnOpportunitiesItemUM(
            id = "${value.cryptoCurrency.id.value}_${value.earnToken.type}",
            currencyIconState = cryptoCurrencyToIconStateConverter.convert(value.cryptoCurrency),
            tokenName = TextReference.Str(value.earnToken.tokenName),
            symbol = TextReference.Str(value.earnToken.tokenSymbol),
            earnType = value.earnToken.type.toTitleText(),
            earnValue = value.earnToken.earnValueText(),
            onItemClick = { onItemClick(value) },
        )
    }
}