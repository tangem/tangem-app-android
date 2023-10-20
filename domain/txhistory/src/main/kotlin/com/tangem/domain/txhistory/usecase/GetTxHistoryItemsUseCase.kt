package com.tangem.domain.txhistory.usecase

import androidx.paging.PagingData
import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

private const val DEFAULT_PAGE_SIZE = 50
// [REDACTED_TODO_COMMENT]
class GetTxHistoryItemsUseCase(private val repository: TxHistoryRepository) {
// [REDACTED_TODO_COMMENT]
    operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        refresh: Boolean = false,
    ): Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>> {
        return either {
            repository
                .getTxHistoryItems(userWalletId, currency, pageSize, refresh)
                .catch { raise(TxHistoryListError.DataError(it)) }
        }
    }
}
