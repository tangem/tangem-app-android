package com.tangem.data.managetokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.isSupportedInApp
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.getBlockchain
import com.tangem.data.common.currency.hasCoinForToken
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.data.managetokens.utils.ManagedCryptoCurrencyFactory
import com.tangem.data.tokens.utils.UserTokensBackwardCompatibility
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.TangemExpressValues
import com.tangem.datasource.api.express.models.request.AssetsRequestBody
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.model.TokenInfo
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

@Suppress("LongParameterList")
internal class DefaultManageTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletManagerFacade: WalletManagersFacade,
    private val tangemExpressApi: TangemExpressApi,
    private val expressAssetsStore: ExpressAssetsStore,
) : ManageTokensRepository {

    private val managedCryptoCurrencyFactory = ManagedCryptoCurrencyFactory()
    private val cryptoCurrencyFactory = CryptoCurrencyFactory()
    private val userTokensResponseFactory = UserTokensResponseFactory()
    private val userTokensBackwardCompatibility = UserTokensBackwardCompatibility()

    // region getTokenListBatchFlow
    override fun getTokenListBatchFlow(
        context: ManageTokensListBatchingContext,
        batchSize: Int,
    ): ManageTokensListBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { it.size.inc() },
            batchFetcher = createFetcher(batchSize),
            updateFetcher = manageTokensUpdateFetcher,
        ).toBatchFlow()
    }

    @Suppress("ComplexCondition")
    private fun createFetcher(
        batchSize: Int,
    ): LimitOffsetBatchFetcher<ManageTokensListConfig, List<ManagedCryptoCurrency>> = LimitOffsetBatchFetcher(
        prefetchDistance = batchSize,
        batchSize = batchSize,
        subFetcher = { request, _, isFirstBatchFetching ->
            val userWallet = request.params.userWalletId?.let { getUserWallet(it) }
            val supportedBlockchains = getSupportedBlockchains(userWallet)
            val searchText = request.params.searchText?.takeIf { it.isNotBlank() }

            val call = suspend {
                tangemTechApi.getCoins(
                    networkIds = supportedBlockchains.joinToString(
                        separator = ",",
                        transform = Blockchain::toNetworkId,
                    ),
                    active = true,
                    searchText = searchText,
                    offset = request.offset * request.limit,
                    limit = request.limit,
                ).getOrThrow()
            }

            val coinsResponse = if (isFirstBatchFetching) {
                call()
            } else {
                retryOnError(call = call)
            }

            val tokensResponse = getStoredUserTokens(request.params.userWalletId)
            val items = if (isFirstBatchFetching &&
                tokensResponse != null &&
                userWallet != null &&
                request.params.searchText.isNullOrBlank()
            ) {
                managedCryptoCurrencyFactory.createWithCustomTokens(
                    coinsResponse = coinsResponse,
                    tokensResponse = tokensResponse,
                    derivationStyleProvider = userWallet.scanResponse.derivationStyleProvider,
                )
            } else {
                managedCryptoCurrencyFactory.create(
                    coinsResponse = coinsResponse,
                    tokensResponse = tokensResponse,
                    derivationStyleProvider = userWallet?.scanResponse?.derivationStyleProvider,
                )
            }

            BatchFetchResult.Success(
                data = items,
                empty = items.isEmpty(),
                last = items.size < request.limit,
            )
        },
    )

    private suspend fun getStoredUserTokens(userWalletId: UserWalletId?): UserTokensResponse? {
        return if (userWalletId != null) {
            appPreferencesStore.getObjectSyncOrNull(
                key = PreferencesKeys.getUserTokensKey(userWalletId.stringValue),
            )
        } else {
            null
        }
    }

    private suspend fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find a user wallet with provided ID: $userWalletId"
        }
    }

    private fun getSupportedBlockchains(userWallet: UserWallet?): List<Blockchain> {
        return userWallet?.scanResponse?.let {
            it.card.supportedBlockchains(it.cardTypesResolver)
        } ?: Blockchain.entries.filter {
            !it.isTestnet() && it.isSupportedInApp()
        }
    }
    // endregion

    // region addManagedCurrencies
    override suspend fun saveManagedCurrencies(
        userWalletId: UserWalletId,
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ) {
        return withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
            )

            val filteredTokens = currenciesToAdd.mapToTokenDataList()
                .filterNot { tokenData ->
                    val blockchain = getBlockchain(networkId = tokenData.sourceNetwork.network.id)
                    val networkId = blockchain.toNetworkId()
                    val contractAddress = (tokenData.sourceNetwork as? SourceNetwork.Default)?.contractAddress
                    savedCurrencies.tokens.firstOrNull { token ->
                        token.contractAddress == contractAddress &&
                            token.networkId == networkId &&
                            token.derivationPath == tokenData.sourceNetwork.network.derivationPath.value
                    } != null
                }
            val newCoins = createCoinsForNewTokens(
                userWalletId = userWalletId,
                newTokensNetworks = filteredTokens.mapNotNull { tokenData ->
                    (tokenData.sourceNetwork as? SourceNetwork.Default)?.network
                },
                savedCurrencies = savedCurrencies.tokens,
            )
            val tokensToSave = filteredTokens.mapToUserResponseToken()

            val tokensToRemove = currenciesToRemove.mapToTokenDataList()
            removeCurrenciesFromWalletManager(userWalletId = userWalletId, tokensDataList = tokensToRemove)

            val newCurrencies = (newCoins + tokensToSave).distinct()
            val updatedResponse = savedCurrencies.copy(
                tokens = savedCurrencies.tokens + newCurrencies - tokensToRemove.mapToUserResponseToken().toSet(),
            )
            storeAndPushTokens(userWalletId = userWalletId, response = updatedResponse)
            fetchExchangeableUserMarketCoinsByIds(userWalletId = userWalletId, userTokens = updatedResponse)
        }
    }

    private suspend fun storeAndPushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        val compatibleUserTokensResponse = userTokensBackwardCompatibility.applyCompatibilityAndGetUpdated(response)
        appPreferencesStore.storeObject(
            key = PreferencesKeys.getUserTokensKey(userWalletId = userWalletId.stringValue),
            value = compatibleUserTokensResponse,
        )

        pushTokens(userWalletId, response)
    }

    private suspend fun pushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        safeApiCall({ tangemTechApi.saveUserTokens(userWalletId.stringValue, response).bind() }) {
            Timber.e(it, "Unable to save user tokens for: ${userWalletId.stringValue}")
        }
    }

    private suspend fun fetchExchangeableUserMarketCoinsByIds(
        userWalletId: UserWalletId,
        userTokens: UserTokensResponse,
    ) {
        try {
            val tokensList = userTokens.tokens
                .map {
                    LeastTokenInfo(
                        contractAddress = it.contractAddress ?: TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE,
                        network = it.networkId,
                    )
                }

            if (tokensList.isNotEmpty()) {
                val response = tangemExpressApi.getAssets(
                    AssetsRequestBody(
                        tokensList = tokensList,
                    ),
                )

                expressAssetsStore.store(userWalletId, response.getOrThrow())
            }
        } catch (e: Throwable) {
            Timber.e(e, "Unable to fetch assets for: ${userWalletId.stringValue}")
        }
    }

    private suspend fun removeCurrenciesFromWalletManager(
        userWalletId: UserWalletId,
        tokensDataList: List<TokenWithSourceNetwork>,
    ) {
        walletManagerFacade.removeTokensByTokenInfo(
            userWalletId = userWalletId,
            tokenInfos = tokensDataList.mapNotNullTo(hashSetOf()) { tokenData ->
                val contractAddress =
                    (tokenData.sourceNetwork as? SourceNetwork.Default)?.contractAddress ?: return@mapNotNullTo null
                TokenInfo(
                    network = tokenData.sourceNetwork.network,
                    name = tokenData.token.name,
                    symbol = tokenData.token.symbol,
                    contractAddress = contractAddress,
                    decimals = tokenData.sourceNetwork.decimals,
                    id = tokenData.token.id.value,
                )
            },
        )
        walletManagerFacade.remove(
            userWalletId = userWalletId,
            networks = tokensDataList.mapNotNullTo(hashSetOf()) { tokenData ->
                (tokenData.sourceNetwork as? SourceNetwork.Main)?.network
            },
        )
    }

    private suspend fun createCoinsForNewTokens(
        userWalletId: UserWalletId,
        newTokensNetworks: List<Network>,
        savedCurrencies: List<UserTokensResponse.Token>,
    ): List<UserTokensResponse.Token> {
        return newTokensNetworks
            .filterNot { savedCurrencies.hasCoinForToken(it) } // tokens without coins
            .mapNotNull {
                cryptoCurrencyFactory.createCoin(
                    blockchain = getBlockchain(networkId = it.id),
                    extraDerivationPath = it.derivationPath.value,
                    derivationStyleProvider = getUserWallet(userWalletId).scanResponse.derivationStyleProvider,
                )
            }
            .distinct()
            .map(userTokensResponseFactory::createResponseToken)
    }

    private fun List<TokenWithSourceNetwork>.mapToUserResponseToken(): List<UserTokensResponse.Token> {
        return map { tokenData ->
            val blockchain = getBlockchain(tokenData.sourceNetwork.network.id)
            UserTokensResponse.Token(
                id = tokenData.token.id.value,
                networkId = blockchain.toNetworkId(),
                derivationPath = tokenData.sourceNetwork.network.derivationPath.value,
                name = tokenData.token.name,
                symbol = tokenData.token.symbol,
                decimals = tokenData.sourceNetwork.decimals,
                contractAddress = (tokenData.sourceNetwork as? SourceNetwork.Default)?.contractAddress,
            )
        }
    }

    private fun Map<ManagedCryptoCurrency.Token, Set<Network>>.mapToTokenDataList(): List<TokenWithSourceNetwork> {
        return flatMap { (token, networks) ->
            token.availableNetworks
                .filter { sourceNetwork -> networks.contains(sourceNetwork.network) }
                .map { sourceNetwork -> TokenWithSourceNetwork(token = token, sourceNetwork = sourceNetwork) }
        }
    }

    private suspend fun getSavedUserTokensResponseSync(key: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = PreferencesKeys.getUserTokensKey(key.stringValue),
        )
    }
    // endregion

    data class TokenWithSourceNetwork(val token: ManagedCryptoCurrency.Token, val sourceNetwork: SourceNetwork)
}