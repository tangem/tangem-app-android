package com.tangem.features.foryou.impl.analytics

import com.tangem.features.foryou.TokenSummaryComponent

/**
 * The summary token's `Token` and `Blockchain` analytics values.
 *
 * The network is `null` for a [TokenSummaryComponent.Token.Market]: such a token carries only the networks
 * it could be added to, not one the summary is about, so the `Blockchain` param is omitted rather than
 * filled with a guess. Returned as a pair so both values are resolved in one pass and cannot come from
 * different tokens.
 */
internal fun TokenSummaryComponent.Token.toAnalyticsTokenAndNetwork(): Pair<String, String?> = when (this) {
    is TokenSummaryComponent.Token.Portfolio -> cryptoCurrency.symbol to cryptoCurrency.network.name
    is TokenSummaryComponent.Token.Market -> symbol to null
}