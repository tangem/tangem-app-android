package com.tangem.domain.txhistory.usecase

import androidx.paging.PagingData
import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

const val DEFAULT_PAGE_SIZE = 50

// TODO: Add tests
class GetTxHistoryItemsUseCase(private val repository: TxHistoryRepository) {

    // FIXME: Provide UserWalletId
    operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        refresh: Boolean = false,
    ): Either<TxHistoryListError, Flow<PagingData<TxInfo>>> {
        return either {
            repository
                .getTxHistoryItems(userWalletId, currency, pageSize, refresh)
                .catch { raise(TxHistoryListError.DataError(it)) }
        }
    }
}