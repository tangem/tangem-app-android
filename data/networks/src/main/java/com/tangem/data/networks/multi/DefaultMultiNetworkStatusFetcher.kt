package com.tangem.data.networks.multi

import arrow.core.raise.catch
import arrow.core.raise.ensure
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.fetcher.CommonNetworkStatusFetcher
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsCache
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Default implementation of [MultiNetworkStatusFetcher]
 *
 * @property networksStatusesStore      networks statuses store
 * @property cardCryptoCurrencyFactory  card crypto currency factory
 * @property commonNetworkStatusFetcher common network status fetcher
 * @property dispatchers                dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultMultiNetworkStatusFetcher @Inject constructor(
    private val networksStatusesStore: NetworksStatusesStore,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val commonNetworkStatusFetcher: CommonNetworkStatusFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiNetworkStatusFetcher {

    override suspend fun invoke(params: MultiNetworkStatusFetcher.Params) = eitherOn(dispatchers.default) {
        networksStatusesStore.setSourceAsCache(userWalletId = params.userWalletId, networks = params.networks)

        val networksCurrencies = catch(
            block = { createNetworksCurrenciesMap(params) },
            catch = {
                networksStatusesStore.setSourceAsOnlyCache(
                    userWalletId = params.userWalletId,
                    networks = params.networks,
                )

                raise(it)
            },
        )

        val result = coroutineScope {
            params.networks
                .map { network ->
                    async {
                        commonNetworkStatusFetcher.fetch(
                            userWalletId = params.userWalletId,
                            network = network,
                            networkCurrencies = networksCurrencies[network].orEmpty().toSet(),
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

    private suspend fun createNetworksCurrenciesMap(
        params: MultiNetworkStatusFetcher.Params,
    ): Map<Network, List<CryptoCurrency>> {
        return cardCryptoCurrencyFactory.create(
            userWalletId = params.userWalletId,
            networks = params.networks,
        )
    }
}