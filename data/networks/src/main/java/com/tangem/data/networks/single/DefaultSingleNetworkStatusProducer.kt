package com.tangem.data.networks.single

import arrow.core.Option
import arrow.core.some
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

/**
 * Default implementation of [SingleNetworkStatusProducer]
 *
 * @property params                     params
 * @property multiNetworkStatusSupplier multi network status supplier
 * @property dispatchers                dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleNetworkStatusProducer @AssistedInject constructor(
    @Assisted val params: SingleNetworkStatusProducer.Params,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleNetworkStatusProducer {

    override val fallback: Option<NetworkStatus>
        get() = NetworkStatus(network = params.network, value = NetworkStatus.Unreachable(address = null)).some()

    override fun produce(): Flow<NetworkStatus> {
        return multiNetworkStatusSupplier(
            params = MultiNetworkStatusProducer.Params(params.userWalletId),
        )
            .mapNotNull { statuses ->
                statuses.firstOrNull { it.network == params.network }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleNetworkStatusProducer.Factory {
        override fun create(params: SingleNetworkStatusProducer.Params): DefaultSingleNetworkStatusProducer
    }
}