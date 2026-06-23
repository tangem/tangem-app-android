package com.tangem.data.txhistory.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.converter.ExpressProviderConverter
import com.tangem.data.txhistory.repository.converter.ExpressStatusMapper
import com.tangem.data.txhistory.repository.converter.ExpressOnrampConverter
import com.tangem.data.txhistory.repository.converter.ExpressSwapConverter
import com.tangem.data.txhistory.repository.factory.ExpressTransactionAssetFactory
import com.tangem.data.txhistory.repository.factory.toAssetId
import com.tangem.data.txhistory.repository.paging.TxHistoryPageBatchFetcher
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.TxHistoryListBatchFlow
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import com.tangem.domain.txhistory.model.TxHistoryListConfig
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.utils.SdkPageConverter
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import javax.inject.Inject

internal class RefactoredTxHistoryRepository @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val txHistoryItemsStore: TxHistoryItemsStore,
    private val expressHistoryDao: ExpressHistoryDao,
    private val expressTransactionAssetFactory: ExpressTransactionAssetFactory,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : TxHistoryRepositoryV2 {

    private val sdkPageConverter = SdkPageConverter()
    private val expressProviderConverter = ExpressProviderConverter()
    private val swapConverter = ExpressSwapConverter()
    private val onrampConverter = ExpressOnrampConverter()
    private val TxHistoryListConfig.storeKey get() = TxHistoryItemsStore.Key(userWalletId, currency)

    override fun getExpressHistory(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        fromCreatedAtMillis: Long,
    ): Flow<List<ExpressTx>> = flow {
        val network = currency.network
        val rawNetwork = network.rawId
        val contract = (currency as? CryptoCurrency.Token)?.contractAddress
            ?: ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE

        // bound filters the window directly in SQL. Generated in the same UTC/'Z' shape as stored values.
        val fromCreatedAtIso = DateTime(fromCreatedAtMillis, DateTimeZone.UTC)
            .toString(ISODateTimeFormat.dateTimeNoMillis())

        val address = walletManagersFacade.getDefaultAddress(userWalletId, network).orEmpty()

        val flow = combine(
            flow = expressHistoryDao.observeOutgoingSwaps(
                ownerAddress = address,
                network = rawNetwork,
                contract = contract,
                fromCreatedAtIso = fromCreatedAtIso,
                activeStatuses = ExpressStatusMapper.activeExchangeStatuses,
            ).distinctUntilChanged(),
            flow2 = expressHistoryDao.observeIncomingSwaps(
                network = rawNetwork,
                contract = contract,
                fromCreatedAtIso = fromCreatedAtIso,
                activeStatuses = ExpressStatusMapper.activeExchangeStatuses,
            ).distinctUntilChanged(),
            flow3 = expressHistoryDao.observeIncomingOnramps(
                ownerAddress = address,
                network = rawNetwork,
                contract = contract,
                fromCreatedAtIso = fromCreatedAtIso,
                activeStatuses = ExpressStatusMapper.activeOnrampStatuses,
            ).distinctUntilChanged(),
            flow4 = expressHistoryDao.getProvidersById().distinctUntilChanged(),
            transform = { outgoingSwaps, incomingSwaps, onramps, providers ->
                buildExpressHistory(
                    userWalletId = userWalletId,
                    outgoingSwaps = outgoingSwaps,
                    incomingSwaps = incomingSwaps,
                    onramps = onramps,
                    providers = providers,
                )
            },
        )
        emitAll(flow)
    }.flowOn(dispatchers.io)

    private suspend fun buildExpressHistory(
        userWalletId: UserWalletId,
        outgoingSwaps: List<ExpressExchangeEntity>,
        incomingSwaps: List<ExpressExchangeEntity>,
        onramps: List<ExpressOnrampEntity>,
        providers: Map<String, ExpressProviderEntity>,
    ): List<ExpressTx> {
        val currencies = expressTransactionAssetFactory.create(
            userWalletId = userWalletId,
            outgoingSwaps = outgoingSwaps,
            incomingSwaps = incomingSwaps,
            onramps = onramps,
        )
        fun String.expressProvider() = providers[this]?.let(expressProviderConverter::convert)
        return buildList {
            outgoingSwaps.forEach { entity ->
                val input = ExpressSwapConverter.Input(
                    entity = entity,
                    provider = entity.providerId.expressProvider(),
                    isOutgoing = true,
                    fromCurrency = currencies[entity.from.toAssetId()],
                    toCurrency = currencies[entity.to.toAssetId()],
                )
                add(swapConverter.convert(input))
            }
            incomingSwaps.forEach { entity ->
                val input = ExpressSwapConverter.Input(
                    entity = entity,
                    provider = entity.providerId.expressProvider(),
                    isOutgoing = false,
                    fromCurrency = currencies[entity.from.toAssetId()],
                    toCurrency = currencies[entity.to.toAssetId()],
                )
                add(swapConverter.convert(input))
            }
            onramps.forEach { entity ->
                val input = ExpressOnrampConverter.Input(
                    entity = entity,
                    provider = entity.providerId.expressProvider(),
                    toCurrency = currencies[entity.to.toAssetId()],
                )
                add(onrampConverter.convert(input))
            }
        }
            // An exchange row may satisfy both swap queries only in degenerate cases;
            // keep the outgoing interpretation (added first).
            .distinctBy { it.txId }
    }

    override fun getTxHistoryBatchFlow(batchSize: Int, context: TxHistoryListBatchingContext): TxHistoryListBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = createFetcher(batchSize),
        ).toBatchFlow()
    }

    private fun createFetcher(
        batchSize: Int,
    ): TxHistoryPageBatchFetcher<TxHistoryListConfig, PaginationWrapper<TxInfo>> =
        TxHistoryPageBatchFetcher { request, _ ->
            val wrappedItems = loadItems(request, batchSize)
            BatchFetchResult.Success(
                data = wrappedItems,
                empty = wrappedItems.items.isEmpty(),
                last = wrappedItems.nextPage is Page.LastPage,
            )
        }

    private suspend fun loadItems(
        request: TxHistoryPageBatchFetcher.Request<TxHistoryListConfig>,
        batchSize: Int,
    ): PaginationWrapper<TxInfo> {
        cacheRegistry.invokeOnExpire(
            key = getTxHistoryPageKey(request.page, request.params),
            skipCache = request.params.shouldRefresh,
            block = { fetch(request, batchSize) },
        )

        return txHistoryItemsStore.getSync(request.page, request.params)
    }

    private suspend fun fetch(request: TxHistoryPageBatchFetcher.Request<TxHistoryListConfig>, batchSize: Int) {
        val wrappedItems = walletManagersFacade.getTxHistoryItems(
            userWalletId = request.params.userWalletId,
            currency = request.params.currency,
            page = sdkPageConverter.convertBack(request.page),
            pageSize = batchSize,
        )

        txHistoryItemsStore.store(key = request.params.storeKey, value = wrappedItems)
    }

    private suspend fun TxHistoryItemsStore.getSync(
        pageToLoad: Page,
        config: TxHistoryListConfig,
    ): PaginationWrapper<TxInfo> {
        val storedItems = requireNotNull(getSyncOrNull(config.storeKey, pageToLoad)) {
            "The transaction history page #$pageToLoad could not be retrieved"
        }

        return if (pageToLoad is Page.Initial) storedItems.addRecentTransactions(config) else storedItems
    }

    private suspend fun PaginationWrapper<TxInfo>.addRecentTransactions(
        config: TxHistoryListConfig,
    ): PaginationWrapper<TxInfo> {
        val recentItems = walletManagersFacade.getRecentTransactions(
            userWalletId = config.userWalletId,
            currency = config.currency,
        )
            .filterUnconfirmedTransaction()
            .sortedByDescending { it.timestampInMillis }
            .filterIfTxAlreadyAdded(apiItems = items)

        return if (recentItems.isEmpty()) {
            TangemLogger.d("Nothing to add to TxHistory")
            this
        } else {
            TangemLogger.d(
                "Recent transactions were added to TxHistory: ${
                    recentItems.joinToString(
                        prefix = "[",
                        postfix = "]",
                        transform = TxInfo::txHash,
                    )
                }",
            )

            return copy(items = recentItems + items)
        }
    }

    private fun List<TxInfo>.filterUnconfirmedTransaction(): List<TxInfo> {
        return filter { it.status == TxInfo.TransactionStatus.Unconfirmed }
    }

    private fun List<TxInfo>.filterIfTxAlreadyAdded(apiItems: List<TxInfo>): List<TxInfo> {
        return filter { item -> apiItems.none { it.txHash == item.txHash } }
    }

    private fun getTxHistoryPageKey(page: Page, config: TxHistoryListConfig): String {
        return "tx_history_page_${config.currency}_${config.userWalletId}_$page"
    }
}