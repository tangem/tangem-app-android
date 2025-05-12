package com.tangem.data.networks.single

import arrow.core.Either
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.data.tokens.utils.NetworkStatusFactory
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
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

    private val networkStatusFactory = NetworkStatusFactory()

    override suspend fun invoke(params: SingleNetworkStatusFetcher.Params) = Either.catchOn(dispatchers.default) {
        val networkCurrencies = when (params) {
            is SingleNetworkStatusFetcher.Params.Prepared -> params.addedNetworkCurrencies
            is SingleNetworkStatusFetcher.Params.Simple -> {
                networksStatusesStore.refresh(userWalletId = params.userWalletId, network = params.network)

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

        val status = networkStatusFactory.createNetworkStatus(
            network = params.network,
            result = result,
            currencies = networkCurrencies.toSet(),
        )

        val statusValue = status.value
        if (statusValue is NetworkStatus.Unreachable) {
            val prevStatus = networksStatusesStore.getSyncOrNull(
                userWalletId = params.userWalletId,
                network = params.network,
            )

            if (prevStatus?.value is NetworkStatus.MissedDerivation) {
                networksStatusesStore.storeUnreachableStatus(userWalletId = params.userWalletId, value = status)
            } else {
                networksStatusesStore.storeError(
                    userWalletId = params.userWalletId,
                    network = params.network,
                    value = statusValue,
                )
            }
        } else {
            networksStatusesStore.storeSuccess(userWalletId = params.userWalletId, value = status)
        }
    }
        .onLeft {
            Timber.e("Failed to fetch network status for $params: $it")
            networksStatusesStore.storeError(userWalletId = params.userWalletId, network = params.network)
        }
}