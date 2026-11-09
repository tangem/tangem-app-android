package com.tangem.data.txhistory.fetcher

import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTriggerInstance
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.retryThreeTimes
import com.tangem.data.txhistory.repository.DefaultExpressHistoryRepository
import com.tangem.data.txhistory.repository.ExpressOperationId
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
import kotlinx.coroutines.flow.*
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

    private val pullStatusJobs = ConcurrentHashMap<ExpressOperationId, PullStatus>()
    private val pullingJobs = ConcurrentHashMap<CryptoCurrency.ID, Job>()

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
                    is TxHistoryFetchTrigger.OnExpressOperationExecute -> {
                        when (trigger.type) {
                            TxHistoryFetchTrigger.OnExpressOperationExecute.Onramp -> fetchOnramp()
                            TxHistoryFetchTrigger.OnExpressOperationExecute.Swap -> fetchExchange()
                        }
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
        val runningJob = pullingJobs[key]
        if (isForce) runningJob?.cancel()
        if (runningJob?.isActive == true) return

        val pullingJob = holdActiveStatuses(walletId, currency, isForce).launchIn(modelScope)
        pullingJobs[key] = pullingJob
        pullingJob.invokeOnCompletion { pullingJobs.remove(key, pullingJob) }
    }

    private fun holdActiveStatuses(
        walletId: UserWalletId,
        currency: CryptoCurrency,
        isForce: Boolean,
    ): Flow<Set<ExpressOperationId>> {
        val heldIds = mutableSetOf<ExpressOperationId>()
        var shouldRestart = isForce

        return observeActiveIds(walletId, currency)
            .onEach { activeIds ->
                if (shouldRestart) {
                    activeIds.forEach { id -> restartPulling(walletId, id) }
                    shouldRestart = false
                }
                heldIds.minus(activeIds).forEach(::releasePulling)
                activeIds.minus(heldIds).forEach { id -> acquirePulling(walletId, id) }
                heldIds.clear()
                heldIds += activeIds
            }
            .onCompletion {
                heldIds.forEach(::releasePulling)
                heldIds.clear()
            }
            .retryThreeTimes()
            .flowOn(dispatchers.io)
    }

    private fun observeActiveIds(walletId: UserWalletId, currency: CryptoCurrency) = flow {
        exchangeDeltaPaginationJob?.join()
        onrampDeltaPaginationJob?.join()

        emitAll(
            expressHistoryRepository
                .observeActiveOperationIds(walletId, currency)
                .distinctUntilChanged(),
        )
    }

    private fun acquirePulling(walletId: UserWalletId, id: ExpressOperationId) {
        pullStatusJobs.compute(id) { _, current ->
            if (current == null || !current.job.isActive) {
                PullStatus(job = launchPulling(walletId, id), holders = 1)
            } else {
                current.copy(holders = current.holders + 1)
            }
        }
    }

    private fun releasePulling(id: ExpressOperationId) {
        pullStatusJobs.computeIfPresent(id) { _, current ->
            val holders = current.holders - 1

            if (holders > 0) {
                current.copy(holders = holders)
            } else {
                current.job.cancel()
                null
            }
        }
    }

    private fun restartPulling(walletId: UserWalletId, id: ExpressOperationId) {
        pullStatusJobs.computeIfPresent(id) { _, current ->
            current.job.cancel()
            current.copy(job = launchPulling(walletId, id))
        }
    }

    private fun launchPulling(walletId: UserWalletId, id: ExpressOperationId) = fetcherScope.launch {
        while (isActive) {
            expressHistoryRepository.refreshActiveStatuses(walletId, id)
            delay(POLL_INTERVAL)
        }
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

    private data class PullStatus(
        val job: Job,
        val holders: Int,
    )

    private companion object {
        val POLL_INTERVAL = 10.seconds
    }
}