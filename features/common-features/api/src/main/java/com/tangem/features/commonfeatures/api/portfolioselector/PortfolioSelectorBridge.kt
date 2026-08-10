package com.tangem.features.commonfeatures.api.portfolioselector

import kotlinx.coroutines.CoroutineScope

/**
 * A [PortfolioFetcher] a caller builds itself and hands to a flow that would otherwise build its own.
 *
 * The flow then sees only the portfolios this fetcher loads. A caller that has already settled on a wallet
 * creates the bridge with [PortfolioFetcher.Mode.Wallet] and the selector has nothing else to offer, so the
 * user cannot complete the flow on a wallet the caller is not waiting for.
 *
 * Narrowing the load is not the same as disabling rows: with a single wallet and a single account there is
 * nothing left to choose and the flow skips the selector entirely.
 */
interface PortfolioSelectorBridge : PortfolioFetcher {

    interface Factory {
        fun create(mode: PortfolioFetcher.Mode, scope: CoroutineScope): PortfolioSelectorBridge
    }
}