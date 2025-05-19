package com.tangem.data.networks.multi

import arrow.core.raise.catch
import arrow.core.raise.ensure
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.fetcher.CommonNetworkStatusFetcher
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.data.networks.store.setSourceAsCache
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.core.utils.eitherOn
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
 * @property userWalletsStore           user wallets store
 * @property cardCryptoCurrencyFactory  card crypto currency factory
 * @property commonNetworkStatusFetcher common network status fetcher
 * @property dispatchers                dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultMultiNetworkStatusFetcher @Inject constructor(
    private val networksStatusesStore: NetworksStatusesStoreV2,
    private val userWalletsStore: UserWalletsStore,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val commonNetworkStatusFetcher: CommonNetworkStatusFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiNetworkStatusFetcher {

    override suspend fun invoke(params: MultiNetworkStatusFetcher.Params) = eitherOn(dispatchers.default) {
        networksStatusesStore.setSourceAsCache(userWalletId = params.userWalletId, networks = params.networks)

        val userWallet = catch(
            block = { userWalletsStore.getSyncStrict(key = params.userWalletId) },
            catch = {
                networksStatusesStore.setSourceAsOnlyCache(
                    userWalletId = params.userWalletId,
                    networks = params.networks,
                )

                raise(it)
            },
        )

        val cardTypesResolver = userWallet.cardTypesResolver
        val isWalletSupported = with(cardTypesResolver) {
            isMultiwalletAllowed() || isSingleWalletWithToken()
        }

        ensure(isWalletSupported) {
            networksStatusesStore.setSourceAsOnlyCache(
                userWalletId = params.userWalletId,
                networks = params.networks,
            )
            IllegalStateException("User wallet is not multi-currency")
        }

        val networksCurrencies = if (cardTypesResolver.isMultiwalletAllowed()) {
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(
                userWallet = userWallet,
                networks = params.networks,
            )
        } else {
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(
                scanResponse = userWallet.scanResponse,
            )
                .groupBy { it.network }
        }

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
}