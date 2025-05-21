package com.tangem.data.networks.store

import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import timber.log.Timber

/**
 * Store actual network [status] by [userWalletId].
 * If [status]'s source is not [StatusSource.ACTUAL], throws exception.
 */
internal suspend fun NetworksStatusesStoreV2.storeStatus(userWalletId: UserWalletId, status: NetworkStatus) {
    if (status.value is NetworkStatus.Unreachable) {
        val prevStatus = getSyncOrNull(userWalletId = userWalletId, network = status.network)

        /**
         * Specific logic!!!
         * If the previous status is [NetworkStatus.MissedDerivation] and current status is [NetworkStatus.Unreachable],
         * then just store [NetworkStatus.MissedDerivation].
         */
        if (prevStatus?.value is NetworkStatus.MissedDerivation) {
            store(userWalletId = userWalletId, status = status)
        } else {
            setSourceAsOnlyCache(
                userWalletId = userWalletId,
                network = status.network,
                value = requireNotNull(status.value as? NetworkStatus.Unreachable),
            )
        }
    } else {
        storeSuccess(userWalletId = userWalletId, status = status)
    }
}

/**
 * Store actual success [status] by [userWalletId].
 * If [status]'s value is [NetworkStatus.Unreachable] and/or source is not [StatusSource.ACTUAL], throws exception.
 */
internal suspend fun NetworksStatusesStoreV2.storeSuccess(userWalletId: UserWalletId, status: NetworkStatus) {
    if (status.value is NetworkStatus.Unreachable) {
        val message = "Use storeError method to save unreachable status"
        Timber.d(message)

        error(message)
    }

    if (status.value.source != StatusSource.ACTUAL) {
        val message = "Method storeActual can be called only with StatusSource.ACTUAL"
        Timber.d(message)

        error(message)
    }

    store(userWalletId = userWalletId, status = status)
}

/** Set [StatusSource] as [StatusSource.CACHE] for [network] by [userWalletId] */
internal suspend fun NetworksStatusesStoreV2.setSourceAsCache(userWalletId: UserWalletId, network: Network) {
    setSourceAsCache(userWalletId = userWalletId, networks = setOf(network))
}

/** Set [StatusSource] as [StatusSource.CACHE] for [networks] by [userWalletId] */
internal suspend fun NetworksStatusesStoreV2.setSourceAsCache(userWalletId: UserWalletId, networks: Set<Network>) {
    updateStatusSource(userWalletId = userWalletId, networks = networks, source = StatusSource.CACHE)
}

/**
 * Set [StatusSource] as [StatusSource.ONLY_CACHE] for [network] by [userWalletId].
 * If the stored status is not found, store the [value] or a default [NetworkStatus.Unreachable] status.
 */
internal suspend fun NetworksStatusesStoreV2.setSourceAsOnlyCache(
    userWalletId: UserWalletId,
    network: Network,
    value: NetworkStatus.Unreachable? = null,
) {
    updateStatusSource(
        userWalletId = userWalletId,
        network = network,
        source = StatusSource.ONLY_CACHE,
        ifNotFound = { id ->
            value?.let { SimpleNetworkStatus(id = id, value = value) }
                ?: createUnreachableStatus(id = id)
        },
    )
}

/**
 * Set [StatusSource] as [StatusSource.ONLY_CACHE] for [networks] by [userWalletId].
 * If the stored status is not found, store a default [NetworkStatus.Unreachable] status.
 */
internal suspend fun NetworksStatusesStoreV2.setSourceAsOnlyCache(userWalletId: UserWalletId, networks: Set<Network>) {
    updateStatusSource(
        userWalletId = userWalletId,
        networks = networks,
        source = StatusSource.ONLY_CACHE,
        ifNotFound = ::createUnreachableStatus,
    )
}

private fun createUnreachableStatus(id: Network.ID): SimpleNetworkStatus {
    return SimpleNetworkStatus(id = id, value = NetworkStatus.Unreachable(address = null))
}