package com.tangem.data.txhistory.fetcher

import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTriggerInstance
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.retryThreeTimes
import com.tangem.data.txhistory.repository.DefaultExpressHistoryRepository
import com.tangem.datasource.api.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.datasource.api.express.models.response.ExchangeHistoryResponse
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryResponse
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.ExpressTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryExpressTrigger
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class DefaultExpressTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val address: String,
    @Assisted private val accountId: AccountId,
    private val utils: TxHistoryFetcherUtils,
    private val expressSyncStateDao: ExpressSyncStateDao,
    private val expressHistoryRepository: DefaultExpressHistoryRepository,
) : ExpressTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    private val userWalletId: UserWalletId get() = accountId.userWalletId

    private var exchangeInitialPaginationJob: Job? = null
    private var exchangeDeltaPaginationJob: Job? = null

    private var onrampInitialPaginationJob: Job? = null
    private var onrampDeltaPaginationJob: Job? = null

    init {
        val receiveFlow = receiveTriggerInstance<TxHistoryExpressTrigger>()
            .onEach { trigger ->
                when (trigger) {
                    is TxHistoryFetchTrigger.TokenDetailsOpen,
                    is TxHistoryFetchTrigger.TokenDetailsPTR,
                    -> {
                        fetchExchange()
                        fetchOnramp()
                    }
                }
            }
        defaultLaunchIn(receiveFlow)
    }

    override suspend fun invoke(params: TxHistoryExpressTrigger) {
        utils.sendTrigger(params)
    }

    override fun close() {
        cancelScope()
    }

    private fun fetchExchange() {
        if (exchangeDeltaPaginationJob?.isActive == true) return
        exchangeDeltaPaginationJob = fetcherScope.launch {
            val isFirstFetch = expressSyncState() == null

            if (isFirstFetch) {
                flow { emit(expressHistoryRepository.fetchExchangeHistory(address, userWalletId)) }
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
                flow { emit(expressHistoryRepository.fetchExchangeHistory(address, userWalletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return
            hasMore = pageResult.pagination.hasMore
        }
    }

    private suspend fun expressDeltaPagination() {
        var hasMore = true
        while (hasMore) {
            val pageResult: ExchangeHistoryDeltaResponse =
                flow { emit(expressHistoryRepository.fetchExchangeHistoryDelta(address, userWalletId)) }
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
                flow { emit(expressHistoryRepository.fetchOnrampHistory(address, userWalletId)) }
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
                flow { emit(expressHistoryRepository.fetchOnrampHistory(address, userWalletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return
            hasMore = pageResult.pagination.hasMore
        }
    }

    private suspend fun onrampDeltaPagination() {
        var hasMore = true
        while (hasMore) {
            val pageResult: OnrampHistoryDeltaResponse =
                flow { emit(expressHistoryRepository.fetchOnrampHistoryDelta(address, userWalletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return
            hasMore = pageResult.pagination.hasMore
        }
    }

    private suspend fun expressSyncState(): ExpressSyncStateEntity? = expressSyncStateDao
        .observe(ExpressSyncStateEntity.Type.EXCHANGE.name, address)
        .first()

    private suspend fun onrampSyncState(): ExpressSyncStateEntity? = expressSyncStateDao
        .observe(ExpressSyncStateEntity.Type.ONRAMP.name, address)
        .first()

    @AssistedFactory
    internal interface Factory {
        fun create(address: String, accountId: AccountId): DefaultExpressTxHistoryFetcher
    }
}