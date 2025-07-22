package com.tangem.domain.networks.multi

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Producer of all networks statuses for wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface MultiNetworkStatusProducer : FlowProducer<Set<NetworkStatus>> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, MultiNetworkStatusProducer>
}