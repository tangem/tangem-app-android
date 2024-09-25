package com.tangem.data.managetokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.l2BlockchainsCoinIds
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.isSupportedInApp
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.currency.getBlockchain
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.data.managetokens.utils.ManagedCryptoCurrencyFactory
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.extensions.supportedTokens
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.managetokens.model.*
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

@Suppress("LongParameterList")
internal class DefaultManageTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : ManageTokensRepository {

    private val managedCryptoCurrencyFactory = ManagedCryptoCurrencyFactory()

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
            // filter l2 coins
            val updatedCoinsResponse = coinsResponse.copy(
                coins = coinsResponse.coins.filterNot { l2BlockchainsCoinIds.contains(it.id) },
            )

            val tokensResponse = request.params.userWalletId?.let { getSavedUserTokensResponseSync(it) }
            val items = if (isFirstBatchFetching &&
                tokensResponse != null &&
                userWallet != null &&
                request.params.searchText.isNullOrBlank()
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

            BatchFetchResult.Success(
                data = items,
                empty = items.isEmpty(),
                last = items.size < request.limit,
            )
        },
    )

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
        val userWallet = getUserWallet(userWalletId = userWalletId)
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
        val userWallet = getUserWallet(userWalletId = userWalletId)
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
        )

        return if (!canHandleBlockchain) {
            CurrencyUnsupportedState.UnsupportedNetwork(networkName = blockchain.getNetworkName())
        } else {
            null
        }
    }

    private fun checkTokenUnsupportedState(
        userWallet: UserWallet,
        blockchain: Blockchain,
    ): CurrencyUnsupportedState.Token? {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver
        val supportedTokens = userWallet.scanResponse.card.supportedTokens(cardTypesResolver)

        return when {
            // refactor this later by moving all this logic in card config
            blockchain == Blockchain.Solana && !supportedTokens.contains(Blockchain.Solana) -> {
                CurrencyUnsupportedState.Token.NetworkTokensUnsupported(networkName = blockchain.getNetworkName())
            }
            !userWallet.scanResponse.card.canHandleToken(supportedTokens, blockchain, cardTypesResolver) -> {
                CurrencyUnsupportedState.Token.UnsupportedCurve(networkName = blockchain.getNetworkName())
            }
            else -> null
        }
    }
}