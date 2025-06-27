package com.tangem.data.networks.fetcher

import arrow.core.Either
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.data.networks.store.storeStatus
import com.tangem.data.networks.utils.NetworkStatusFactory
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Common implementation of network status fetcher
 *
 * @property walletManagersFacade  wallet managers facade
 * @property networksStatusesStore networks statuses store
 * @property dispatchers           dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class CommonNetworkStatusFetcher @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val networksStatusesStore: NetworksStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Fetch
     *
     * @param userWalletId      user wallet id
     * @param network           network
     * @param networkCurrencies network currencies
     */
    suspend fun fetch(
        userWalletId: UserWalletId,
        network: Network,
        networkCurrencies: Set<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val result = withContext(dispatchers.io) {
                walletManagersFacade.update(
                    userWalletId = userWalletId,
                    network = network,
                    extraTokens = networkCurrencies
                        .filterIsInstance<CryptoCurrency.Token>()
                        .toSet(),
                )
            }

            val status = NetworkStatusFactory.create(
                network = network,
                updatingResult = result,
                addedCurrencies = networkCurrencies,
            )

            networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status)
        }
            .onLeft {
                Timber.e("Failed to fetch network status for $userWalletId [${network.rawId}]: $it")
                networksStatusesStore.setSourceAsOnlyCache(userWalletId = userWalletId, network = network)
            }
    }
}