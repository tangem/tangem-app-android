package com.tangem.data.managetokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.l2BlockchainsCoinIds
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.data.managetokens.utils.ManagedCryptoCurrencyFactory
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.orDefault
import com.tangem.datasource.local.config.testnet.TestnetTokensStorage
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.card.common.TapWorkarounds.isTestCard
import com.tangem.domain.card.common.extensions.*
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.managetokens.model.*
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher.Request
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

@Suppress("LongParameterList", "LargeClass")
internal class DefaultManageTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val userTokenSaver: UserTokensSaver,
    private val manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val testnetTokensStorage: TestnetTokensStorage,
    private val excludedBlockchains: ExcludedBlockchains,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    networkFactory: NetworkFactory,
) : ManageTokensRepository {

    private val managedCryptoCurrencyFactory = ManagedCryptoCurrencyFactory(
        networkFactory = networkFactory,
        excludedBlockchains = excludedBlockchains,
        accountsFeatureToggles = accountsFeatureToggles,
    )
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

            if (userWallet is UserWallet.Cold && userWallet.scanResponse.card.isTestCard) {
                when (val params = request.params) {
                    is ManageTokensListConfig.Account -> fetchTestnetCurrencies(userWallet, params)
                    is ManageTokensListConfig.Wallet -> fetchTestnetCurrenciesLegacy(userWallet, params)
                }
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

        val items = when (val params = request.params) {
            is ManageTokensListConfig.Account -> createManagedCryptoCurrencyList(
                params = params,
                userWallet = userWallet,
                isFirstBatchFetching = isFirstBatchFetching,
                loadUserTokensFromRemote = loadUserTokensFromRemote,
                query = query,
                updatedCoinsResponse = updatedCoinsResponse,
            )
            is ManageTokensListConfig.Wallet -> createManagedCryptoCurrencyListLegacy(
                params = params,
                userWallet = userWallet,
                isFirstBatchFetching = isFirstBatchFetching,
                loadUserTokensFromRemote = loadUserTokensFromRemote,
                query = query,
                updatedCoinsResponse = updatedCoinsResponse,
            )
        }

        return BatchFetchResult.Success(
            data = items,
            empty = items.isEmpty(),
            last = items.size < request.limit,
        )
    }

    private suspend fun createManagedCryptoCurrencyList(
        params: ManageTokensListConfig.Account,
        userWallet: UserWallet?,
        isFirstBatchFetching: Boolean,
        loadUserTokensFromRemote: Boolean,
        query: String?,
        updatedCoinsResponse: CoinsResponse,
    ): List<ManagedCryptoCurrency> {
        val response = params.userWalletId?.let { userWalletId ->
            if (loadUserTokensFromRemote && userWallet != null) {
                runCatching { walletAccountsFetcher.fetch(userWalletId = userWallet.walletId) }.getOrNull()
            } else {
                walletAccountsFetcher.getSaved(userWalletId)
            }
        }

        val accountId = when {
            params.accountId == null -> null
            loadUserTokensFromRemote -> {
                AccountId.forCryptoPortfolio(
                    userWalletId = requireNotNull(params.accountId).userWalletId,
                    derivationIndex = DerivationIndex.Main,
                )
            }
            else -> requireNotNull(params.accountId)
        }

        val accountDTO = if (response != null && accountId != null) {
            response.accounts.firstOrNull { it.id == accountId.value }
        } else {
            null
        }

        val tokensResponse = response?.let {
            UserTokensResponse(
                group = response.wallet.group.orDefault(),
                sort = response.wallet.sort.orDefault(),
                tokens = accountDTO?.tokens.orEmpty(),
            )
        }

        val isCreateWithCustom = isFirstBatchFetching &&
            tokensResponse != null &&
            userWallet != null &&
            query == null

        val items = if (isCreateWithCustom) {
            managedCryptoCurrencyFactory.createWithCustomTokens(
                coinsResponse = updatedCoinsResponse,
                tokensResponse = tokensResponse,
                userWallet = userWallet,
                accountIndex = accountDTO?.derivationIndex?.let(DerivationIndex::invoke)?.getOrNull(),
            )
        } else {
            managedCryptoCurrencyFactory.create(
                coinsResponse = updatedCoinsResponse,
                tokensResponse = tokensResponse,
                userWallet = userWallet,
                accountIndex = accountDTO?.derivationIndex?.let(DerivationIndex::invoke)?.getOrNull(),
            )
        }

        return items
    }

    private suspend fun createManagedCryptoCurrencyListLegacy(
        params: ManageTokensListConfig.Wallet,
        userWallet: UserWallet?,
        isFirstBatchFetching: Boolean,
        loadUserTokensFromRemote: Boolean,
        query: String?,
        updatedCoinsResponse: CoinsResponse,
    ): List<ManagedCryptoCurrency> {
        val tokensResponse = params.userWalletId?.let { userWalletId ->
            if (loadUserTokensFromRemote && userWallet != null) {
                safeApiCall({ tangemTechApi.getUserTokens(userWalletId.stringValue).bind() }) {
                    // save tokens response only if loadUserTokensFromRemote is true and it means onboarding call
                    createAndSaveDefaultUserTokensResponse(userWallet = userWallet)
                }
            } else {
                getSavedUserTokensResponseSync(userWalletId)
            }
        }

        val isCreateWithCustom = isFirstBatchFetching &&
            tokensResponse != null &&
            userWallet != null &&
            query == null

        return if (isCreateWithCustom) {
            managedCryptoCurrencyFactory.createWithCustomTokens(
                coinsResponse = updatedCoinsResponse,
                tokensResponse = tokensResponse,
                userWallet = userWallet,
                accountIndex = null,
            )
        } else {
            managedCryptoCurrencyFactory.create(
                coinsResponse = updatedCoinsResponse,
                tokensResponse = tokensResponse,
                userWallet = userWallet,
                accountIndex = null,
            )
        }
    }

    private suspend fun createAndSaveDefaultUserTokensResponse(userWallet: UserWallet): UserTokensResponse {
        val userTokensResponse = createDefaultUserTokensResponse(userWallet)
        userTokenSaver.store(userWallet.walletId, userTokensResponse, useEnricher = false)
        return userTokensResponse
    }

    private suspend fun fetchTestnetCurrencies(
        userWallet: UserWallet,
        params: ManageTokensListConfig.Account,
    ): BatchFetchResult.Success<List<ManagedCryptoCurrency>> {
        val searchText = params.searchText
        val testnetTokensConfig = testnetTokensStorage.getConfig()

        val response = params.userWalletId?.let { userWalletId ->
            walletAccountsFetcher.getSaved(userWalletId)
        }

        val accountId = params.accountId

        val accountDTO = if (response != null && accountId != null) {
            response.accounts.firstOrNull { it.id == accountId.value }
        } else {
            null
        }

        val tokensResponse = response?.let {
            UserTokensResponse(
                group = response.wallet.group.orDefault(),
                sort = response.wallet.sort.orDefault(),
                tokens = accountDTO?.tokens.orEmpty(),
            )
        }

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
            tokensResponse = tokensResponse,
            userWallet = userWallet,
            accountIndex = accountDTO?.derivationIndex?.let(DerivationIndex::invoke)?.getOrNull(),
        )

        return BatchFetchResult.Success(
            data = items,
            empty = items.isEmpty(),
            last = true,
        )
    }

    private suspend fun fetchTestnetCurrenciesLegacy(
        userWallet: UserWallet,
        params: ManageTokensListConfig.Wallet,
    ): BatchFetchResult.Success<List<ManagedCryptoCurrency>> {
        val searchText = params.searchText
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
            userWallet = userWallet,
            accountIndex = null,
        )

        return BatchFetchResult.Success(
            data = items,
            empty = items.isEmpty(),
            last = true,
        )
    }

    private fun createDefaultUserTokensResponse(userWallet: UserWallet) =
        userTokensResponseFactory.createUserTokensResponse(
            currencies = cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(
                userWallet = userWallet,
            ),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

    private fun getSupportedBlockchains(userWallet: UserWallet?): List<Blockchain> {
        return userWallet?.supportedBlockchains(excludedBlockchains)
            ?: Blockchain.entries.filter {
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
                    val networkId = sourceNetwork.network.toBlockchain().toNetworkId()

                    UserTokensResponse.Token(
                        id = token.id.value,
                        networkId = networkId,
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
        return userTokensResponseStore.getSyncOrNull(userWalletId = key)
    }

    override suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        sourceNetwork: SourceNetwork,
    ): CurrencyUnsupportedState? {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)
        val blockchain = sourceNetwork.id.toBlockchain()

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
        val canHandleBlockchain = userWallet.canHandleBlockchain(
            blockchain = blockchain,
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
        if (userWallet is UserWallet.Hot) {
            return if (blockchain in hotWalletExcludedBlockchains) {
                CurrencyUnsupportedState.Token.NetworkTokensUnsupported(networkName = blockchain.fullName)
            } else {
                null
            }
        }

        if (userWallet !is UserWallet.Cold) {
            return null
        }

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