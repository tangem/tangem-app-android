package com.tangem.data.txhistory.fetcher

import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTriggerInstance
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.retryThreeTimes
import com.tangem.data.txhistory.repository.DefaultJointOperationHistoryRepository
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.JointOperationTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryJointOperationTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class DefaultJointOperationTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val walletId: UserWalletId,
    private val utils: TxHistoryFetcherUtils,
    private val repository: DefaultJointOperationHistoryRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
) : JointOperationTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    private var initialPaginationJob: Job? = null
    private var deltaPaginationJob: Job? = null

    init {
        defaultLaunchIn(receiveTriggerInstance<TxHistoryJointOperationTrigger>().onEach { fetch() })
    }

    override fun invoke(params: TxHistoryJointOperationTrigger) {
        utils.sendTrigger(params)
    }

    override fun close() {
        cancelScope()
    }

    private fun fetch() {
        if (deltaPaginationJob?.isActive == true) return
        deltaPaginationJob = fetcherScope.launch {
            if (!isEnabled()) return@launch

            if (repository.syncState(walletId) == null) {
                flow { emit(repository.fetchHistory(walletId)) }
                    .retryThreeTimes()
                    .firstOrNull() ?: return@launch
            }

            if (initialPaginationJob?.isActive != true) {
                initialPaginationJob = fetcherScope.launch { initialPagination() }
            }

            deltaPagination()
        }
    }

    private suspend fun isEnabled(): Boolean {
        return singleAccountListSupplier.getSyncOrNull(walletId)?.accounts.orEmpty().any { it is Account.Joint }
    }

    private suspend fun initialPagination() {
        if (repository.syncState(walletId)?.isInitialCompleted == true) return
        var hasMore = true
        while (hasMore) {
            val page = flow { emit(repository.fetchHistory(walletId)) }
                .retryThreeTimes()
                .firstOrNull() ?: return
            hasMore = page.pagination.hasMore
        }
    }

    private suspend fun deltaPagination() {
        var hasMore = true
        while (hasMore) {
            val page = flow { emit(repository.fetchHistoryDelta(walletId)) }
                .retryThreeTimes()
                .firstOrNull() ?: return
            hasMore = page.pagination.hasMore
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(walletId: UserWalletId): DefaultJointOperationTxHistoryFetcher
    }
}