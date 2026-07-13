package com.tangem.features.commonfeatures.api.choosetoken

import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo

/**
 * A token offered for adding to the portfolio inside the token chooser, independent of any
 * campaign/promo model. Callers convert their own models (e.g. promo payout tokens) into this type,
 * so the chooser stays agnostic of feature-specific sources.
 *
 * Exactly one [network] per token — the chooser renders one "add" row per [PredefinedTokenToAdd].
 * [TokenMarketInfo.Network.decimalCount] must be resolved by the caller — a network without decimals
 * cannot be added and must be dropped before reaching the chooser.
 */
data class PredefinedTokenToAdd(
    val token: RawMarketToken,
    val network: TokenMarketInfo.Network,
    val iconUrl: String? = null,
)