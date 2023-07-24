package com.tangem.domain.txhistory.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.recover
import arrow.core.right
import com.tangem.domain.txhistory.error.TxHistoryListError
import com.tangem.domain.txhistory.model.TxHistoryItem
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class GetTxHistoryItemsUseCase(
    private val repository: TxHistoryRepository,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) {

    operator fun invoke(
        networkId: String,
        derivationPath: String,
        page: Int,
        pageSize: Int,
    ): Flow<Either<TxHistoryListError, List<TxHistoryItem>>> {
        return channelFlow {
            recover(
                block = {
                    getTxHistoryItems(networkId, derivationPath, page, pageSize)
                        .collect { items -> send(items.right()) }
                },
                recover = { error -> send(error.left()) }
            )
        }
    }

    private fun Raise<TxHistoryListError>.getTxHistoryItems(
        networkId: String,
        derivationPath: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<TxHistoryItem>> {
        return repository.getTxHistoryItems(
            networkId = networkId,
            derivationPath = derivationPath,
            page = page,
            pageSize = pageSize,
        )
            .catch { raise(TxHistoryListError.DataError(it)) }
            .flowOn(dispatcherProvider.io)
    }
}
