package com.tangem.domain.networks.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Fetcher of network status [Network] for wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleNetworkStatusFetcher : FlowFetcher<SingleNetworkStatusFetcher.Params> {

    data class Params(val userWalletId: UserWalletId, val network: Network)
}