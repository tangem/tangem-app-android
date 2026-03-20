package com.tangem.features.feed.model.market.details.converter

import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.markets.TokenMarketExchange
import com.tangem.domain.markets.TokenMarketExchange.TrustScore
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.ExchangeItemUM
import com.tangem.utils.converter.Converter

internal object ExchangeItemStateConverterV2 : Converter<TokenMarketExchange, ExchangeItemUM> {

    override fun convert(value: TokenMarketExchange): ExchangeItemUM.Content {
        return ExchangeItemUM.Content(
            id = value.id,
            title = stringReference(value.name),
            subTitle = stringReference(value = if (value.isCentralized) "CEX" else "DEX"),
            icon = TangemIconUM.Currency(
                CurrencyIconState.CoinIcon(
                    url = value.imageUrl,
                    fallbackResId = R.drawable.ic_alert_24,
                    isGrayscale = false,
                    shouldShowCustomBadge = false,
                ),
            ),
            volumeInUsd = stringReference(
                value.volumeInUsd.format {
                    fiat(
                        fiatCurrencyCode = "USD",
                        fiatCurrencySymbol = "$",
                    ).compact()
                },
            ),
            auditLabel = value.trustScore.toAuditLabelUM(),
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