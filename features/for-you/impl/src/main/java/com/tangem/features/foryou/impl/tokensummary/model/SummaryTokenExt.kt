package com.tangem.features.foryou.impl.tokensummary.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.foryou.TokenSummaryComponent

/** Raw id of the summary token. Absent for a custom token, which has no market identity. */
internal val TokenSummaryComponent.Token.rawCurrencyId: CryptoCurrency.RawID?
    get() = when (this) {
        is TokenSummaryComponent.Token.Portfolio -> cryptoCurrency.id.rawCurrencyId
        is TokenSummaryComponent.Token.Market -> cryptoCurrencyRawId
    }

/**
 * Network of the summary token, set only for a Portfolio-origin token — the summary was opened for that very network,
 * so its holdings are narrowed down to it. A Market-origin token spans every network it is held on.
 */
internal val TokenSummaryComponent.Token.network: Network?
    get() = (this as? TokenSummaryComponent.Token.Portfolio)?.cryptoCurrency?.network