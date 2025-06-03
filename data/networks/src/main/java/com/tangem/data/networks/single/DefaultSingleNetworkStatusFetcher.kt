package com.tangem.data.networks.single

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.data.tokens.utils.CardCryptoCurrenciesFactory
import com.tangem.data.tokens.utils.NetworkStatusFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [SingleNetworkStatusFetcher]
 *
 * @param excludedBlockchains      excluded blockchains
 * @property walletManagersFacade  wallet managers facade
 * @property networksStatusesStore networks statuses store
 * @property userWalletsStore      user wallets store
 * @property appPreferencesStore   app preferences store
 * @property dispatchers           dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleNetworkStatusFetcher @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
    private val walletManagersFacade: WalletManagersFacade,
    private val networksStatusesStore: NetworksStatusesStoreV2,
    private val userWalletsStore: UserWalletsStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleNetworkStatusFetcher {

    private val demoConfig = DemoConfig()
    private val cardCurrenciesFactory = CardCryptoCurrenciesFactory(demoConfig, excludedBlockchains)
    private val responseCurrenciesFactory = ResponseCryptoCurrenciesFactory(excludedBlockchains)
    private val networkStatusFactory = NetworkStatusFactory()

    override suspend fun invoke(params: SingleNetworkStatusFetcher.Params) = Either.catchOn(dispatchers.default) {
        val networkCurrencies = when (params) {
            is SingleNetworkStatusFetcher.Params.Prepared -> params.addedNetworkCurrencies
            is SingleNetworkStatusFetcher.Params.Simple -> {
                networksStatusesStore.refresh(userWalletId = params.userWalletId, network = params.network)

                val userWallet = userWalletsStore.getSyncStrict(key = params.userWalletId)

                createCurrencies(userWallet = userWallet, network = params.network)
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

    private suspend fun createCurrencies(userWallet: UserWallet, network: Network): List<CryptoCurrency> {
        val blockchain = Blockchain.fromNetworkId(networkId = network.backendId)

        // multi-currency wallet
        if (userWallet.isMultiCurrency) return getMultiWalletCurrencies(userWallet = userWallet, network = network)

        // check if the blockchain of single-currency wallet is the same as network
        val cardBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
        if (cardBlockchain != blockchain) return emptyList()

        // single-currency wallet with token (NODL)
        if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
            return cardCurrenciesFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse)
        }

        // single-currency wallet
        return cardCurrenciesFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse).let(::listOf)
    }

    private suspend fun getMultiWalletCurrencies(userWallet: UserWallet, network: Network): List<CryptoCurrency> {
        val response = appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = PreferencesKeys.getUserTokensKey(userWallet.walletId.stringValue),
        ) ?: return emptyList()

        return responseCurrenciesFactory.createCurrencies(
            tokens = response.tokens.filter {
                it.networkId == network.backendId && it.derivationPath == network.derivationPath.value
            },
            scanResponse = userWallet.scanResponse,
        )
    }
}