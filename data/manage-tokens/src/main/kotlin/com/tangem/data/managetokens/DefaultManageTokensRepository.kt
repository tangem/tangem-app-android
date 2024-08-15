package com.tangem.data.managetokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.isSupportedInApp
import com.tangem.blockchainsdk.utils.toNetworkId
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
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

internal class DefaultManageTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val managedCryptoCurrencyFactory: ManagedCryptoCurrencyFactory,
    private val manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : ManageTokensRepository {

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
            val userWallet = getUserWallet(request.params.userWalletId)
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

    private suspend fun getUserWallet(userWalletId: UserWalletId?): UserWallet? {
        if (userWalletId == null) {
            return null
        }

        return requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "User wallet not found"
        }
    }

    private fun getSupportedBlockchains(userWallet: UserWallet?): List<Blockchain> {
        return userWallet?.scanResponse?.let {
            it.card.supportedBlockchains(it.cardTypesResolver)
        } ?: Blockchain.entries.filter {
            !it.isTestnet() && it.isSupportedInApp()
        }
    }
}