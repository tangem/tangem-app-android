package com.tangem.data.networks.multi

import arrow.core.raise.catch
import arrow.core.raise.ensure
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Default implementation of [MultiNetworkStatusFetcher]
 *
 * @property singleNetworkStatusFetcher single network status fetcher
 * @property networksStatusesStore      networks statuses store
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultMultiNetworkStatusFetcher @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
    private val networksStatusesStore: NetworksStatusesStoreV2,
    private val userWalletsStore: UserWalletsStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
) : MultiNetworkStatusFetcher {

    private val responseCurrenciesFactory by lazy { ResponseCryptoCurrenciesFactory(excludedBlockchains) }

    override suspend fun invoke(params: MultiNetworkStatusFetcher.Params) = eitherOn(dispatchers.default) {
        // Optimization!
        // Every singleNetworkStatusFetcher with applyRefresh as true will refresh every network in the store.
        // So if we update all networks at once, it will be more efficient.
        networksStatusesStore.refresh(userWalletId = params.userWalletId, networks = params.networks)

        val userWallet = catch(
            block = { userWalletsStore.getSyncStrict(key = params.userWalletId) },
            catch = {
                networksStatusesStore.storeError(userWalletId = params.userWalletId, networks = params.networks)
                raise(it)
            },
        )

        val isNotSingleWallet = with(userWallet.scanResponse.cardTypesResolver) {
            isMultiwalletAllowed() || isSingleWalletWithToken()
        }

        ensure(isNotSingleWallet) {
            networksStatusesStore.storeError(userWalletId = params.userWalletId, networks = params.networks)
            IllegalStateException("User wallet is not multi-currency")
        }

        val networksCurrencies = createCurrencies(userWallet = userWallet, networks = params.networks)

        val result = coroutineScope {
            params.networks
                .map {
                    async {
                        singleNetworkStatusFetcher(
                            params = SingleNetworkStatusFetcher.Params.Prepared(
                                userWalletId = params.userWalletId,
                                network = it,
                                addedNetworkCurrencies = networksCurrencies,
                            ),
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

    private suspend fun createCurrencies(userWallet: UserWallet, networks: Set<Network>): Set<CryptoCurrency> {
        val blockchains = networks.map { Blockchain.fromNetworkId(networkId = it.backendId) }

        // multi-currency wallet
        if (userWallet.isMultiCurrency) return getMultiWalletCurrencies(userWallet = userWallet, networks = networks)

        // check if the blockchain of single-currency wallet is the same as network
        val cardBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
        if (!blockchains.contains(cardBlockchain)) return emptySet()

        return cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse).toSet()
    }

    private suspend fun getMultiWalletCurrencies(userWallet: UserWallet, networks: Set<Network>): Set<CryptoCurrency> {
        val response = appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = PreferencesKeys.getUserTokensKey(userWallet.walletId.stringValue),
        ) ?: return emptySet()

        return responseCurrenciesFactory.createCurrencies(
            tokens = response.tokens.filter { token ->
                networks.any { it.backendId == token.networkId && it.derivationPath.value == token.derivationPath }
            },
            scanResponse = userWallet.scanResponse,
        )
            .toSet()
    }
}