package com.tangem.data.networks.single

import arrow.core.Either
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.fetcher.CommonNetworkStatusFetcher
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.data.networks.store.setSourceAsCache
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

/**
 * Default implementation of [SingleNetworkStatusFetcher]
 *
 * @property commonNetworkStatusFetcher common network status fetcher
 * @property networksStatusesStore      networks statuses store
 * @property cardCryptoCurrencyFactory  card crypto currency factory
 * @property dispatchers                dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleNetworkStatusFetcher @Inject constructor(
    private val commonNetworkStatusFetcher: CommonNetworkStatusFetcher,
    private val networksStatusesStore: NetworksStatusesStoreV2,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleNetworkStatusFetcher {

    override suspend fun invoke(params: SingleNetworkStatusFetcher.Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            networksStatusesStore.setSourceAsCache(userWalletId = params.userWalletId, network = params.network)

            val networkCurrencies = cardCryptoCurrencyFactory.create(
                userWalletId = params.userWalletId,
                network = params.network,
            )

            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = params.network,
                networkCurrencies = networkCurrencies.toSet(),
            )
                .onLeft { throw it }
        }
    }
}