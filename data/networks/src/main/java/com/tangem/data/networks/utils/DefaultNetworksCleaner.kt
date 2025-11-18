package com.tangem.data.networks.utils

import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        withContext(dispatchers.default) {
            val (networks, tokens) = currencies.partitionByType()

            coroutineScope {
                launch { cleanStore(userWalletId = userWalletId, networks = networks) }
                launch { cleanWalletManager(userWalletId = userWalletId, networks = networks, tokens = tokens) }
            }
        }
    }

    private suspend fun cleanStore(userWalletId: UserWalletId, networks: Set<Network>) {
        if (networks.isNotEmpty()) {
            networksStatusesStore.clear(userWalletId = userWalletId, networks = networks)
        }
    }

    private suspend fun cleanWalletManager(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        tokens: Set<CryptoCurrency.Token>,
    ) {
        if (networks.isNotEmpty()) {
            walletManagersFacade.remove(userWalletId = userWalletId, networks = networks)
        }

        if (tokens.isNotEmpty()) {
            walletManagersFacade.removeTokens(userWalletId = userWalletId, tokens = tokens)
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