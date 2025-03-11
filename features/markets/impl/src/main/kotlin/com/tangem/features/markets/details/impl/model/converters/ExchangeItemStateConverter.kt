package com.tangem.features.markets.details.impl.model.converters

import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.markets.TokenMarketExchange
import com.tangem.domain.markets.TokenMarketExchange.TrustScore
import com.tangem.features.markets.impl.R
import com.tangem.utils.converter.Converter

/**
 * Converter from [TokenMarketExchange] to [TokenItemState]
 *
[REDACTED_AUTHOR]
 */
internal object ExchangeItemStateConverter : Converter<TokenMarketExchange, TokenItemState> {

    override fun convert(value: TokenMarketExchange): TokenItemState {
        return TokenItemState.Content(
            id = value.id,
            iconState = CurrencyIconState.CoinIcon(
                url = value.imageUrl,
                fallbackResId = R.drawable.ic_alert_24,
                isGrayscale = false,
                showCustomBadge = false,
            ),
            titleState = TokenItemState.TitleState.Content(text = stringReference(value.name)),
            fiatAmountState = TokenItemState.FiatAmountState.Content(
                text = BigDecimalFormatter.formatFiatPriceUncapped(
                    fiatAmount = value.volumeInUsd,
                    fiatCurrencyCode = "USD",
                    fiatCurrencySymbol = "$",
                ),
            ),
            subtitleState = TokenItemState.SubtitleState.TextContent(
                value = stringReference(value = if (value.isCentralized) "CEX" else "DEX"),
            ),
            subtitle2State = TokenItemState.Subtitle2State.LabelContent(
                auditLabelUM = value.trustScore.toAuditLabelUM(),
            ),
            onItemClick = null,
            onItemLongClick = null,
        )
    }

    private fun TrustScore.toAuditLabelUM(): AuditLabelUM {
        return when (this) {
            TrustScore.Risky -> AuditLabelUM(
                text = resourceReference(id = R.string.markets_token_details_exchange_trust_score_risky),
                type = AuditLabelUM.Type.Prohibition,
            )
            TrustScore.Caution -> AuditLabelUM(
                text = resourceReference(id = R.string.markets_token_details_exchange_trust_score_caution),
                type = AuditLabelUM.Type.Warning,
            )
            TrustScore.Trusted -> AuditLabelUM(
                text = resourceReference(id = R.string.markets_token_details_exchange_trust_score_trusted),
                type = AuditLabelUM.Type.Permit,
            )
        }
    }
}