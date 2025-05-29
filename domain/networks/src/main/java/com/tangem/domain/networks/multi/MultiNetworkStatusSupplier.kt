package com.tangem.domain.networks.multi

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.network.NetworkStatus

/**
 * Supplier of all networks statuses for selected wallet [MultiNetworkStatusProducer.Params]
 *
 * @property factory    factory for creating [MultiNetworkStatusProducer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class MultiNetworkStatusSupplier(
    override val factory: MultiNetworkStatusProducer.Factory,
    override val keyCreator: (MultiNetworkStatusProducer.Params) -> String,
) : FlowCachingSupplier<MultiNetworkStatusProducer, MultiNetworkStatusProducer.Params, Set<NetworkStatus>>()