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

    /**
     * Params
     *
     * @property userWalletId wallet id
     * @property network      network
     * @property applyRefresh flag that determines whether to apply refresh (see DefaultMultiNetworkStatusFetcher)
     */
    data class Params(
        val userWalletId: UserWalletId,
        val network: Network,
        val applyRefresh: Boolean = true,
    )
}