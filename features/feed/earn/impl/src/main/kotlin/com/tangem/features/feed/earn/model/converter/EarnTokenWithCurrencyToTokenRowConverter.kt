package com.tangem.features.feed.earn.model.converter

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.utils.converter.Converter

internal class EarnTokenWithCurrencyToTokenRowConverter(
    private val onItemClick: (EarnTokenWithCurrency) -> Unit,
) : Converter<EarnTokenWithCurrency, TangemTokenRow.State.Content> {

    override fun convert(value: EarnTokenWithCurrency): TangemTokenRow.State.Content {
        return TangemTokenRow.State.Content(
            // the raw API triple, not the CryptoCurrency id: that one is built from a canonicalised
            // blockchain and drops the token id for coins, so two rows can share it and collide as keys
            id = "${value.earnToken.tokenId}_${value.earnToken.networkId}_${value.earnToken.type}",
            icon = TangemTokenIcon.UiState.Token(
                tokenState = TangemTokenIcon.State(
                    url = value.cryptoCurrency.iconUrl,
                    // a coin's own icon already is the network icon, so only tokens carry the badge
                    topIcon = (value.cryptoCurrency as? CryptoCurrency.Token)?.networkIconResId,
                ),
            ),
            title = stringReference(value.earnToken.tokenName),
            quote = stringReference(value.networkName),
            fiatBalance = value.earnToken.earnValueText(),
            cryptoBalance = value.earnToken.type.toTitleText(),
            onClick = { onItemClick(value) },
        )
    }
}