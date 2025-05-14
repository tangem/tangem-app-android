package com.tangem.data.networks.single

import arrow.core.Either
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.data.networks.store.setSourceAsCache
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.data.networks.store.storeStatus
import com.tangem.data.networks.utils.NetworkStatusFactory
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [SingleNetworkStatusFetcher]
 *
 * @property walletManagersFacade      wallet managers facade
 * @property networksStatusesStore     networks statuses store
 * @property cardCryptoCurrencyFactory card crypto currency factory
 * @property dispatchers               dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleNetworkStatusFetcher @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val networksStatusesStore: NetworksStatusesStoreV2,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleNetworkStatusFetcher {

    override suspend fun invoke(params: SingleNetworkStatusFetcher.Params) = Either.catchOn(dispatchers.default) {
        val networkCurrencies = when (params) {
            is SingleNetworkStatusFetcher.Params.Prepared -> params.addedNetworkCurrencies
            is SingleNetworkStatusFetcher.Params.Simple -> {
                networksStatusesStore.setSourceAsCache(userWalletId = params.userWalletId, network = params.network)

                cardCryptoCurrencyFactory.create(
                    userWalletId = params.userWalletId,
                    network = params.network,
                )
            }
        }

        val result = withContext(dispatchers.io) {
            walletManagersFacade.update(
                userWalletId = params.userWalletId,
                network = params.network,
                extraTokens = networkCurrencies
                    .filterIsInstance<CryptoCurrency.Token>()
                    .toSet(),
            )
        }

        val status = NetworkStatusFactory.create(
            network = params.network,
            updatingResult = result,
            addedCurrencies = networkCurrencies.toSet(),
        )

        networksStatusesStore.storeStatus(userWalletId = params.userWalletId, status = status)
    }
        .onLeft {
            Timber.e("Failed to fetch network status for $params: $it")
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = params.userWalletId, network = params.network)
        }
}