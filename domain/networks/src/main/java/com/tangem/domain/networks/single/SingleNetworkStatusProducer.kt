package com.tangem.domain.networks.single

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Producer of network status [Network] for wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleNetworkStatusProducer : FlowProducer<NetworkStatus> {

    data class Params(val userWalletId: UserWalletId, val network: Network)

    interface Factory : FlowProducer.Factory<Params, SingleNetworkStatusProducer>
}