package com.tangem.domain.txhistory.usecase

import androidx.paging.PagingData
import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

private const val DEFAULT_PAGE_SIZE = 20

class GetTxHistoryItemsUseCase(private val repository: TxHistoryRepository) {

    operator fun invoke(
        currency: CryptoCurrency,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>> {
        return either {
            repository
                .getTxHistoryItems(currency = currency, pageSize = pageSize)
                .catch { raise(TxHistoryListError.DataError(it)) }
        }
    }
}
