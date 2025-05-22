package com.tangem.data.managetokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.l2BlockchainsCoinIds
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.getBlockchain
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.data.managetokens.utils.ManagedCryptoCurrencyFactory
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.config.testnet.TestnetTokensStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.extensions.supportedTokens
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.managetokens.model.*
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher.Request
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

@Suppress("LongParameterList")
internal class DefaultManageTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
    private val appPreferencesStore: AppPreferencesStore,
    private val testnetTokensStorage: TestnetTokensStorage,
    private val excludedBlockchains: ExcludedBlockchains,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val dispatchers: CoroutineDispatcherProvider,
    networkFactory: NetworkFactory,
) : ManageTokensRepository {

    private val managedCryptoCurrencyFactory = ManagedCryptoCurrencyFactory(networkFactory, excludedBlockchains)
    private val userTokensResponseFactory = UserTokensResponseFactory()

    // region getTokenListBatchFlow
    override fun getTokenListBatchFlow(
        context: ManageTokensListBatchingContext,
        loadUserTokensFromRemote: Boolean,
        batchSize: Int,
    ): ManageTokensListBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { it.size.inc() },
            batchFetcher = createFetcher(batchSize, loadUserTokensFromRemote),
            updateFetcher = manageTokensUpdateFetcher,
        ).toBatchFlow()
    }

    private fun createFetcher(
        batchSize: Int,
        loadUserTokensFromRemote: Boolean,
    ): LimitOffsetBatchFetcher<ManageTokensListConfig, List<ManagedCryptoCurrency>> = LimitOffsetBatchFetcher(
        prefetchDistance = batchSize,
        batchSize = batchSize,
        subFetcher = { request, _, isFirstBatchFetching ->
            val userWallet = request.params.userWalletId?.let(userWalletsStore::getSyncStrict)

            if (userWallet?.scanResponse?.card?.isTestCard == true) {
                fetchTestnetCurrencies(userWallet, request)
            } else {
                fetchCurrencies(
                    userWallet = userWallet,
                    request = request,
                    isFirstBatchFetching = isFirstBatchFetching,
                    loadUserTokensFromRemote = loadUserTokensFromRemote,
                )
            }
        },
    )

    @Suppress("ComplexCondition")
    private suspend fun fetchCurrencies(
        userWallet: UserWallet?,
        request: Request<ManageTokensListConfig>,
        isFirstBatchFetching: Boolean,
        loadUserTokensFromRemote: Boolean,
    ): BatchFetchResult.Success<List<ManagedCryptoCurrency>> {
        val supportedBlockchains = getSupportedBlockchains(userWallet)
        val query = request.params.searchText.takeUnless(String?::isNullOrBlank)

        val call = suspend {
            tangemTechApi.getCoins(
                networkIds = supportedBlockchains.joinToString(
                    separator = ",",
                    transform = Blockchain::toNetworkId,
                ),
                active = true,
                searchText = query,
                offset = request.offset,
                limit = request.limit,
            ).getOrThrow()
        }

        val coinsResponse = if (isFirstBatchFetching) {
            call()
        } else {
            retryOnError(call = call)
        }
        // filter l2 coins
        val updatedCoinsResponse = coinsResponse.copy(
            coins = coinsResponse.coins.filterNot { l2BlockchainsCoinIds.contains(it.id) },
        )

        val tokensResponse = request.params.userWalletId?.let { userWalletId ->
            if (loadUserTokensFromRemote && userWallet != null) {
                safeApiCall({ tangemTechApi.getUserTokens(userWalletId.stringValue).bind() }) {
                    createDefaultUserTokensResponse(userWallet)
                }
            } else {
                getSavedUserTokensResponseSync(userWalletId)
            }
        }
        val items = if (isFirstBatchFetching &&
            tokensResponse != null &&
            userWallet != null &&
            query == null
        ) {
            managedCryptoCurrencyFactory.createWithCustomTokens(
                coinsResponse = updatedCoinsResponse,
                tokensResponse = tokensResponse,
                scanResponse = userWallet.scanResponse,
            )
        } else {
            managedCryptoCurrencyFactory.create(
                coinsResponse = updatedCoinsResponse,
                tokensResponse = tokensResponse,
                scanResponse = userWallet?.scanResponse,
            )
        }

        return BatchFetchResult.Success(
            data = items,
            empty = items.isEmpty(),
            last = items.size < request.limit,
        )
    }

    private suspend fun fetchTestnetCurrencies(
        userWallet: UserWallet,
        request: Request<ManageTokensListConfig>,
    ): BatchFetchResult.Success<List<ManagedCryptoCurrency>> {
        val searchText = request.params.searchText
        val testnetTokensConfig = testnetTokensStorage.getConfig()

        val items = managedCryptoCurrencyFactory.createTestnetWithCustomTokens(
            testnetTokensConfig = if (!searchText.isNullOrBlank()) {
                testnetTokensConfig.copy(
                    tokens = testnetTokensConfig.tokens.filter { token ->
                        token.symbol.contains(other = searchText, ignoreCase = true) ||
                            token.name.contains(other = searchText, ignoreCase = true)
                    },
                )
            } else {
                testnetTokensConfig
            },
            tokensResponse = getSavedUserTokensResponseSync(userWallet.walletId),
            scanResponse = userWallet.scanResponse,
        )

        return BatchFetchResult.Success(
            data = items,
            empty = items.isEmpty(),
            last = true,
        )
    }

    private fun createDefaultUserTokensResponse(userWallet: UserWallet) =
        userTokensResponseFactory.createUserTokensResponse(
            currencies = cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(userWallet.scanResponse),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

    private fun getSupportedBlockchains(userWallet: UserWallet?): List<Blockchain> {
        return userWallet?.scanResponse?.let {
            it.card.supportedBlockchains(it.cardTypesResolver, excludedBlockchains)
        } ?: Blockchain.entries.filter {
            !it.isTestnet() && it !in excludedBlockchains
        }
    }
    // endregion

    override suspend fun hasLinkedTokens(
        userWalletId: UserWalletId,
        network: Network,
        tempAddedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        tempRemovedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Boolean {
        val addedTokens = tempAddedTokens.mapToResponseTokens()
        val removedTokens = tempRemovedTokens.mapToResponseTokens()

        val storedTokens = requireNotNull(
            value = getSavedUserTokensResponseSync(userWalletId),
            lazyMessage = { "Unable to find tokens response for user wallet with provided ID: $userWalletId" },
        )
        val newTokensList = storedTokens.tokens + addedTokens - removedTokens.toSet()

        return newTokensList.any {
            it.contractAddress != null &&
                it.networkId == network.backendId &&
                it.derivationPath == network.derivationPath.value
        }
    }

    private fun Map<ManagedCryptoCurrency.Token, Set<Network>>.mapToResponseTokens(): List<UserTokensResponse.Token> {
        return flatMap { (token, networks) ->
            token.availableNetworks
                .filter { sourceNetwork -> networks.contains(sourceNetwork.network) }
                .map { sourceNetwork ->
                    val blockchain = getBlockchain(sourceNetwork.network.id)
                    UserTokensResponse.Token(
                        id = token.id.value,
                        networkId = blockchain.toNetworkId(),
                        derivationPath = sourceNetwork.network.derivationPath.value,
                        name = token.name,
                        symbol = token.symbol,
                        decimals = sourceNetwork.decimals,
                        contractAddress = (sourceNetwork as? SourceNetwork.Default)?.contractAddress,
                    )
                }
        }
    }

    private suspend fun getSavedUserTokensResponseSync(key: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = PreferencesKeys.getUserTokensKey(key.stringValue),
        )
    }

    override suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        sourceNetwork: SourceNetwork,
    ): CurrencyUnsupportedState? {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)
        val blockchain = getBlockchain(sourceNetwork.id)
        return when (sourceNetwork) {
            is SourceNetwork.Default -> checkTokenUnsupportedState(userWallet = userWallet, blockchain = blockchain)
            is SourceNetwork.Main -> checkBlockchainUnsupportedState(userWallet = userWallet, blockchain = blockchain)
        }
    }

    override suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        rawNetworkId: String,
        isMainNetwork: Boolean,
    ): CurrencyUnsupportedState? {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)
        val blockchain = Blockchain.fromNetworkId(networkId = rawNetworkId)
            ?: error("Can not create blockchain with given networkId -> $rawNetworkId")
        return if (isMainNetwork) {
            checkBlockchainUnsupportedState(userWallet, blockchain)
        } else {
            checkTokenUnsupportedState(userWallet, blockchain)
        }
    }

    private fun checkBlockchainUnsupportedState(
        userWallet: UserWallet,
        blockchain: Blockchain,
    ): CurrencyUnsupportedState? {
        val canHandleBlockchain = userWallet.scanResponse.card.canHandleBlockchain(
            blockchain = blockchain,
            cardTypesResolver = userWallet.cardTypesResolver,
            excludedBlockchains = excludedBlockchains,
        )

        return if (!canHandleBlockchain) {
            CurrencyUnsupportedState.UnsupportedNetwork(networkName = blockchain.fullName)
        } else {
            null
        }
    }

    private fun checkTokenUnsupportedState(
        userWallet: UserWallet,
        blockchain: Blockchain,
    ): CurrencyUnsupportedState.Token? {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver
        val supportedTokens = userWallet.scanResponse.card.supportedTokens(
            cardTypesResolver,
            excludedBlockchains,
        )

        return when {
            // refactor this later by moving all this logic in card config
            blockchain == Blockchain.Solana && !supportedTokens.contains(Blockchain.Solana) -> {
                CurrencyUnsupportedState.Token.NetworkTokensUnsupported(networkName = blockchain.fullName)
            }
            !userWallet.scanResponse.card.canHandleToken(supportedTokens, blockchain, cardTypesResolver) -> {
                CurrencyUnsupportedState.Token.UnsupportedCurve(networkName = blockchain.fullName)
            }
            else -> null
        }
    }
}