package com.tangem.domain.networks.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Fetcher of network status [Network] for wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleNetworkStatusFetcher : FlowFetcher<SingleNetworkStatusFetcher.Params> {

    /**
     * Params
     *
     * @property userWalletId user wallet id
     * @property network      network
     */
    data class Params(val userWalletId: UserWalletId, val network: Network)
}