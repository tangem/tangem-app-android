package com.tangem.domain.txhistory.usecase

import androidx.paging.PagingData
import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.recover
import arrow.core.right
import com.tangem.domain.txhistory.error.TxHistoryListError
import com.tangem.domain.txhistory.model.TxHistoryItem
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect

private const val DEFAULT_PAGE_SIZE = 20

class GetTxHistoryItemsUseCase(private val repository: TxHistoryRepository) {

    operator fun invoke(
        networkId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Flow<Either<TxHistoryListError,
            PagingData<TxHistoryItem>,>,> {
        return channelFlow {
            recover(
                block = {
                    getTxHistoryItems(
                        networkId = networkId,
                        pageSize = pageSize,
                    ).collect { items ->
                        send(items.right())
                    }
                },
                recover = { error -> send(error.left()) },
            )
        }
    }

    private fun Raise<TxHistoryListError>.getTxHistoryItems(
        networkId: String,
        pageSize: Int,
    ): Flow<PagingData<TxHistoryItem>> {
        return repository
            .getTxHistoryItems(networkId = networkId, pageSize = pageSize)
            .catch { raise(TxHistoryListError.DataError(it)) }
    }
}