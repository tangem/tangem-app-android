package com.tangem.domain.polymarket.flow

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Fetcher of the prediction account status of a single wallet.
 *
 * It may be called for any wallet at any time — including one that never opened the feature — so it must reach
 * the network only for what it can answer without asking the user for anything.
 */
interface PredictionAccountStatusFetcher : FlowFetcher<PredictionAccountStatusFetcher.Params> {

    data class Params(val userWalletId: UserWalletId)
}