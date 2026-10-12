package com.tangem.data.networks.fetcher

import arrow.core.Either
import arrow.core.right
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.data.networks.store.storeStatus
import com.tangem.data.networks.utils.NetworkStatusFactory
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
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
     * @param xpub              XPUB to restore dynamic addresses mode
     */
    suspend fun fetch(
        userWalletId: UserWalletId,
        network: Network,
        networkCurrencies: Set<CryptoCurrency>,
        xpub: String? = null,
    ): Either<Throwable, Unit> {
        // Guard: empty networkCurrencies would result in NetworkStatus.Verified(amounts=emptyMap()),
        // which overwrites any valid cached status and leaves all currencies in this network as Loading.
        if (networkCurrencies.isEmpty()) {
            TangemLogger.w("Skipping fetch for $userWalletId [${network.rawId}]: networkCurrencies is empty")
            return Unit.right()
        }

        return Either.catchOn(dispatchers.default) {
            // A pending joint account has no Safe yet, so no wallet manager: there is nothing to read and no error
            // to store. Building the manager can fail, so the lookup stays inside the catch
            if (network.derivationPath is Network.DerivationPath.Joint &&
                walletManagersFacade.getOrCreateWalletManager(userWalletId = userWalletId, network = network) == null
            ) {
                TangemLogger.i(
                    "Skipping fetch for $userWalletId [${network.id}]: the joint account has no Safe address yet",
                )
                return@catchOn
            }

            val extraTokens = networkCurrencies.filterIsInstance<CryptoCurrency.Token>().toSet()

            val result = withContext(dispatchers.io) {
                walletManagersFacade.update(
                    userWalletId = userWalletId,
                    network = network,
                    extraTokens = extraTokens,
                    xpub = xpub,
                )
            }

            val status = NetworkStatusFactory.create(
                network = network,
                updatingResult = result,
                addedCurrencies = networkCurrencies,
            )

            networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status)
        }
            .onLeft { throwable ->
                TangemLogger.e("Failed to fetch network status for $userWalletId [${network.rawId}]", throwable)
                networksStatusesStore.setSourceAsOnlyCache(userWalletId = userWalletId, network = network)
            }
    }
}