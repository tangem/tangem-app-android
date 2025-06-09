package com.tangem.domain.networks.single

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.network.NetworkStatus

/**
 * Supplier of network status for selected wallet [SingleNetworkStatusProducer.Params]
 *
 * @property factory    factory for creating [SingleNetworkStatusProducer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class SingleNetworkStatusSupplier(
    override val factory: SingleNetworkStatusProducer.Factory,
    override val keyCreator: (SingleNetworkStatusProducer.Params) -> String,
) : FlowCachingSupplier<SingleNetworkStatusProducer, SingleNetworkStatusProducer.Params, NetworkStatus>()