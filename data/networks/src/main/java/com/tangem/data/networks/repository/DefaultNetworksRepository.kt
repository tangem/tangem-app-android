package com.tangem.data.networks.repository

import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.storeStatus
import com.tangem.data.networks.utils.NetworkStatusFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Default implementation of [NetworksRepository]
 *
 * @property cardCryptoCurrencyFactory card crypto currency factory
 * @property walletManagersFacade      wallet managers facade
 * @property networksStatusesStore     networks statuses store
 * @property dispatchers               dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultNetworksRepository(
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val walletManagersFacade: WalletManagersFacade,
    private val networksStatusesStore: NetworksStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : NetworksRepository {

    override suspend fun fetchPendingTransactions(userWalletId: UserWalletId, network: Network) {
        withContext(dispatchers.default) {
            val currencies = runCatching {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            }
                .getOrElse {
                    Timber.e(it, "Unable to create wallet currencies")
                    return@withContext
                }

            fetchPendingTransactions(userWalletId = userWalletId, network = network, currencies = currencies)
        }
    }

    override suspend fun getNetworkAddresses(
        userWalletId: UserWalletId,
        network: Network,
    ): List<CryptoCurrencyAddress> {
        return runCatching { cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network) }
            .getOrElse {
                Timber.e(it, "Unable to create wallet currencies")
                return emptyList()
            }
            .map { currency ->
                CryptoCurrencyAddress(
                    cryptoCurrency = currency,
                    address = getDefaultAddress(userWalletId, network),
                )
            }
    }

    private suspend fun fetchPendingTransactions(
        userWalletId: UserWalletId,
        network: Network,
        currencies: List<CryptoCurrency>,
    ) {
        val result = withContext(dispatchers.io) {
            walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
        }

        val networkStatus = NetworkStatusFactory.create(
            network = network,
            updatingResult = result,
            addedCurrencies = currencies.toSet(),
        )

        networksStatusesStore.storeStatus(userWalletId = userWalletId, status = networkStatus)
    }

    private suspend fun getDefaultAddress(userWalletId: UserWalletId, network: Network): String {
        return withContext(dispatchers.io) {
            walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network).orEmpty()
        }
    }
}