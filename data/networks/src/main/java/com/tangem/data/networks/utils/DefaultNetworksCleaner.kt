package com.tangem.data.networks.utils

import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Default implementation of [NetworksCleaner].
 *
 * @property networksStatusesStore Store to manage network statuses.
 * @property walletManagersFacade Facade to manage wallet managers.
 * @property dispatchers Coroutine dispatchers provider.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultNetworksCleaner(
    private val networksStatusesStore: NetworksStatusesStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : NetworksCleaner {

    override suspend fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) {
            Timber.d("No currencies to clear for wallet: $userWalletId")
            return
        }

        withContext(dispatchers.default) {
            val (networks, tokens) = currencies.partitionByType()

            awaitAll(
                async { clearStatusesStore(userWalletId = userWalletId, networks = networks) },
                async { clearBlockchainSDK(userWalletId = userWalletId, networks = networks, tokens = tokens) },
            )
        }
    }

    private suspend fun clearStatusesStore(userWalletId: UserWalletId, networks: Set<Network>) {
        if (networks.isNotEmpty()) {
            runSuspendCatching {
                networksStatusesStore.clear(userWalletId = userWalletId, networks = networks)
            }
                .onFailure { Timber.e(it, "Failed to clear network statuses for wallet: $userWalletId") }
        }
    }

    private suspend fun clearBlockchainSDK(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        tokens: Set<CryptoCurrency.Token>,
    ) {
        if (networks.isNotEmpty()) {
            runSuspendCatching {
                walletManagersFacade.remove(userWalletId = userWalletId, networks = networks)
            }
                .onFailure {
                    Timber.e(it, "Failed to remove networks from Blockchain SDK for wallet: $userWalletId")
                }
        }

        if (tokens.isNotEmpty()) {
            runSuspendCatching {
                walletManagersFacade.removeTokens(userWalletId = userWalletId, tokens = tokens)
            }
                .onFailure {
                    Timber.e(it, "Failed to remove tokens from Blockchain SDK for wallet: $userWalletId")
                }
        }
    }

    private fun List<CryptoCurrency>.partitionByType(): Pair<Set<Network>, Set<CryptoCurrency.Token>> {
        val networks = mutableSetOf<Network>()
        val tokens = mutableSetOf<CryptoCurrency.Token>()

        for (currency in this) {
            when (currency) {
                is CryptoCurrency.Coin -> networks.add(currency.network)
                is CryptoCurrency.Token -> tokens.add(currency)
            }
        }

        return Pair(networks, tokens)
    }
}