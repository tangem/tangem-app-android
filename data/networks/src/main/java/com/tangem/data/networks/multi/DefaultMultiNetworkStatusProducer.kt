package com.tangem.data.networks.multi

import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Default implementation of [MultiNetworkStatusProducer]
 *
 * @property params                params
 * @property networksStatusesStore networks statuses store
 * @property userWalletsStore      user wallets store
 * @property networkFactory        network factory
 * @property dispatchers           dispatchers
 */
internal class DefaultMultiNetworkStatusProducer @AssistedInject constructor(
    @Assisted val params: MultiNetworkStatusProducer.Params,
    private val networksStatusesStore: NetworksStatusesStore,
    private val userWalletsStore: UserWalletsStore,
    private val networkFactory: NetworkFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiNetworkStatusProducer {

    override val fallback: Set<NetworkStatus>
        get() = setOf()

    override fun produce(): Flow<Set<NetworkStatus>> {
        return networksStatusesStore.get(userWalletId = params.userWalletId)
            .distinctUntilChanged()
            .mapNotNull { statuses ->
                val userWallet = userWalletsStore.getSyncOrNull(params.userWalletId)

                if (userWallet == null) {
                    Timber.e("Unable to get UserWallet with provided ID: ${params.userWalletId}")
                    return@mapNotNull null
                }

                statuses.mapNotNullTo(hashSetOf()) { status ->
                    val network = networkFactory.create(
                        networkId = status.id,
                        derivationPath = status.id.derivationPath,
                        scanResponse = userWallet.scanResponse,
                    ) ?: return@mapNotNullTo null

                    NetworkStatus(network = network, value = status.value)
                }
            }
            .distinctUntilChanged()
            .onEmpty { emit(value = hashSetOf()) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiNetworkStatusProducer.Factory {
        override fun create(params: MultiNetworkStatusProducer.Params): DefaultMultiNetworkStatusProducer
    }
}