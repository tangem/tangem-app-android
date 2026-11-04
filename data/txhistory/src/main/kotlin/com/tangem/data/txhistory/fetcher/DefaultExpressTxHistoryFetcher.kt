package com.tangem.data.txhistory.fetcher

import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTriggerInstance
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.retryThreeTimes
import com.tangem.data.txhistory.repository.DefaultExpressHistoryRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.ExpressTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryExpressTrigger
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.grow.datasource.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.grow.datasource.express.models.response.ExchangeHistoryResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryResponse
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

internal class DefaultExpressTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val walletId: UserWalletId,
    private val utils: TxHistoryFetcherUtils,
    private val historySyncStateDao: HistorySyncStateDao,
    private val expressHistoryRepository: DefaultExpressHistoryRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : ExpressTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    private var exchangeInitialPaginationJob: Job? = null
    private var exchangeDeltaPaginationJob: Job? = null

    private var onrampInitialPaginationJob: Job? = null
    private var onrampDeltaPaginationJob: Job? = null

    /**
     * Status polling of the currencies, keyed so one currency is never polled twice.
     *
     * Concurrent because entries are dropped from [Job.invokeOnCompletion], which runs on whatever thread
     * finished the job — not on the trigger loop that puts them in.
     */
    private val pullStatusJobs = ConcurrentHashMap<CryptoCurrency.ID, Job>()

    init {
        val receiveFlow = receiveTriggerInstance<TxHistoryExpressTrigger>()
            .onEach { trigger ->
                when (trigger) {
                    is TxHistoryFetchTrigger.TokenDetailsOpen -> deltaAndPullStatusById(
                        walletId = trigger.walletId,
                        currency = trigger.currency,
                        modelScope = trigger.modelScope,
                        isForce = false,
                    )
                    is TxHistoryFetchTrigger.TokenDetailsPTR -> {
                        deltaAndPullStatusById(
                            walletId = trigger.walletId,
                            currency = trigger.currency,
                            modelScope = trigger.modelScope,
                            isForce = true,
                        )
                    }
                    is TxHistoryFetchTrigger.WalletSelected,
                    -> {
                        fetchExchange()
                        fetchOnramp()
                    }
                }
            }
        defaultLaunchIn(receiveFlow)
    }

    override fun invoke(params: TxHistoryExpressTrigger) {
        utils.sendTrigger(params)
    }

    override fun close() {
        cancelScope()
    }

    private fun deltaAndPullStatusById(
        walletId: UserWalletId,
        currency: CryptoCurrency,
        modelScope: CoroutineScope,
        isForce: Boolean,
    ) {
        fetchExchange()
        fetchOnramp()

        val key = currency.id
        val runningJob = pullStatusJobs[key]
        if (isForce) runningJob?.cancel()
        if (runningJob?.isActive == true) return

        val pullingJob = modelScope.launch(dispatchers.io) {
            exchangeDeltaPaginationJob?.join()
            onrampDeltaPaginationJob?.join()

            var shouldContinuityPulling = true
            while (isActive && shouldContinuityPulling) {
                shouldContinuityPulling = expressHistoryRepository.refreshActiveStatuses(walletId, currency)
                if (shouldContinuityPulling) delay(10.seconds)
            }
        }
        pullStatusJobs[key] = pullingJob
        pullingJob.invokeOnCompletion { pullStatusJobs.remove(key, pullingJob) }
    }

    private fun fetchExchange() {
        if (exchangeDeltaPaginationJob?.isActive == true) return
        exchangeDeltaPaginationJob = fetcherScope.launch {
            val isFirstFetch = expressSyncState() == null

            if (isFirstFetch) {
                flow { emit(expressHistoryRepository.fetchExchangeHistory(walletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return@launch
            }

            if (exchangeInitialPaginationJob?.isActive != true) {
                exchangeInitialPaginationJob = launch { expressInitialPagination() }
            }

            expressDeltaPagination()
        }
    }

    private suspend fun expressInitialPagination() {
        if (expressSyncState()?.isInitialCompleted == true) return
        var hasMore = true
        while (hasMore) {
            val pageResult: ExchangeHistoryResponse =
                flow { emit(expressHistoryRepository.fetchExchangeHistory(walletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return
            hasMore = pageResult.pagination.hasMore
        }
    }

    private suspend fun expressDeltaPagination() {
        var hasMore = true
        while (hasMore) {
            val pageResult: ExchangeHistoryDeltaResponse =
                flow { emit(expressHistoryRepository.fetchExchangeHistoryDelta(walletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return
            hasMore = pageResult.pagination.hasMore
        }
    }

    private fun fetchOnramp() {
        if (onrampDeltaPaginationJob?.isActive == true) return
        onrampDeltaPaginationJob = fetcherScope.launch {
            val isFirstFetch = onrampSyncState() == null

            if (isFirstFetch) {
                flow { emit(expressHistoryRepository.fetchOnrampHistory(walletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return@launch
            }

            if (onrampInitialPaginationJob?.isActive != true) {
                onrampInitialPaginationJob = launch { onrampInitialPagination() }
            }

            onrampDeltaPagination()
        }
    }

    private suspend fun onrampInitialPagination() {
        if (onrampSyncState()?.isInitialCompleted == true) return
        var hasMore = true
        while (hasMore) {
            val pageResult: OnrampHistoryResponse =
                flow { emit(expressHistoryRepository.fetchOnrampHistory(walletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return
            hasMore = pageResult.pagination.hasMore
        }
    }

    private suspend fun onrampDeltaPagination() {
        var hasMore = true
        while (hasMore) {
            val pageResult: OnrampHistoryDeltaResponse =
                flow { emit(expressHistoryRepository.fetchOnrampHistoryDelta(walletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return
            hasMore = pageResult.pagination.hasMore
        }
    }

    private suspend fun expressSyncState(): HistorySyncStateEntity? = historySyncStateDao
        .observe(HistorySyncStateEntity.Type.EXCHANGE.name, walletId.stringValue)
        .first()

    private suspend fun onrampSyncState(): HistorySyncStateEntity? = historySyncStateDao
        .observe(HistorySyncStateEntity.Type.ONRAMP.name, walletId.stringValue)
        .first()

    @AssistedFactory
    internal interface Factory {
        fun create(walletId: UserWalletId): DefaultExpressTxHistoryFetcher
    }
}