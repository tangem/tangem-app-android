package com.tangem.domain.networks.multi

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Fetcher of network status [Network] for multi-currency wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface MultiNetworkStatusFetcher : FlowFetcher<MultiNetworkStatusFetcher.Params> {

    data class Params(val userWalletId: UserWalletId, val networks: Set<Network>)
}