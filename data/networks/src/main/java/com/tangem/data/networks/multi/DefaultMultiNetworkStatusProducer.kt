package com.tangem.data.networks.multi

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.tokens.model.NetworkStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEmpty
import timber.log.Timber

/**
 * Default implementation of [MultiNetworkStatusProducer]
 *
 * @property params                params
 * @property networksStatusesStore networks statuses store
 * @property userWalletsStore      user wallets store
 * @property excludedBlockchains   excluded blockchains
 */
internal class DefaultMultiNetworkStatusProducer @AssistedInject constructor(
    @Assisted val params: MultiNetworkStatusProducer.Params,
    private val networksStatusesStore: NetworksStatusesStoreV2,
    private val userWalletsStore: UserWalletsStore,
    private val excludedBlockchains: ExcludedBlockchains,
) : MultiNetworkStatusProducer {

    override val fallback: Set<NetworkStatus>
        get() = setOf()

    override fun produce(): Flow<Set<NetworkStatus>> {
        return networksStatusesStore
            .get(userWalletId = params.userWalletId)
            .mapNotNull { statuses ->
                val userWallet = userWalletsStore.getSyncOrNull(params.userWalletId)

                if (userWallet == null) {
                    Timber.e("Unable to get UserWallet with provided ID: ${params.userWalletId}")
                    return@mapNotNull null
                }

                statuses.mapNotNullTo(hashSetOf()) { status ->
                    val network = getNetwork(
                        networkId = status.id.networkId,
                        derivationPath = status.id.derivationPath,
                        scanResponse = userWallet.scanResponse,
                        excludedBlockchains = excludedBlockchains,
                    ) ?: return@mapNotNullTo null

                    NetworkStatus(network = network, value = status.value)
                }
            }
            .distinctUntilChanged()
            .onEmpty { emit(value = hashSetOf()) }
    }

    @AssistedFactory
    interface Factory : MultiNetworkStatusProducer.Factory {
        override fun create(params: MultiNetworkStatusProducer.Params): DefaultMultiNetworkStatusProducer
    }
}