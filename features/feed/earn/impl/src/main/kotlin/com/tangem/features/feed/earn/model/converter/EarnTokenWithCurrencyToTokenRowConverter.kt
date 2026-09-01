package com.tangem.features.feed.earn.model.converter

import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.utils.converter.Converter

internal class EarnTokenWithCurrencyToTokenRowConverter(
    private val onItemClick: (EarnTokenWithCurrency) -> Unit,
) : Converter<EarnTokenWithCurrency, TangemTokenRow.State.Content> {

    override fun convert(value: EarnTokenWithCurrency): TangemTokenRow.State.Content {
        return TangemTokenRow.State.Content(
            id = "${value.cryptoCurrency.id.value}_${value.earnToken.type}",
            icon = TangemTokenIcon.UiState.Token(
                tokenState = TangemTokenIcon.State(url = value.cryptoCurrency.iconUrl),
            ),
            title = stringReference(value.earnToken.tokenName),
            quote = stringReference(value.networkName),
            fiatBalance = value.earnToken.earnValueText(),
            cryptoBalance = value.earnToken.type.toTitleText(),
            onClick = { onItemClick(value) },
        )
    }
}