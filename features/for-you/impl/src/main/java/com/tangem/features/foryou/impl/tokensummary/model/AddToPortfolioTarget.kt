package com.tangem.features.foryou.impl.tokensummary.model

import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo

/**
 * What the add-to-portfolio flow needs to offer the summary token: its market identity and the networks to add it on.
 */
internal data class AddToPortfolioTarget(
    val token: RawMarketToken,
    val networks: List<TokenMarketInfo.Network>,
)