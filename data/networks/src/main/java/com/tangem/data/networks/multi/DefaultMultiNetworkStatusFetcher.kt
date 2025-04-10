package com.tangem.data.networks.multi

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Default implementation of [MultiNetworkStatusFetcher]
 *
 * @property singleNetworkStatusFetcher single network status fetcher
 * @property networksStatusesStore      networks statuses store
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiNetworkStatusFetcher @Inject constructor(
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val networksStatusesStore: NetworksStatusesStoreV2,
) : MultiNetworkStatusFetcher {

    override suspend fun invoke(params: MultiNetworkStatusFetcher.Params): Either<Throwable, Unit> = either {
        // Optimization!
        // Every singleNetworkStatusFetcher with applyRefresh as true will refresh every network in the store.
        // So if we update all networks at once, it will be more efficient.
        networksStatusesStore.refresh(userWalletId = params.userWalletId, networks = params.networks)

        val result = coroutineScope {
            params.networks
                .map {
                    async {
                        singleNetworkStatusFetcher(
                            params = SingleNetworkStatusFetcher.Params(
                                userWalletId = params.userWalletId,
                                network = it,
                                applyRefresh = false,
                            ),
                        )
                    }
                }
                .awaitAll()
        }

        val failedResult = result.firstOrNull { it.isLeft() }

        ensure(failedResult == null) {
            IllegalStateException("Failed to fetch network statuses")
        }
    }
}