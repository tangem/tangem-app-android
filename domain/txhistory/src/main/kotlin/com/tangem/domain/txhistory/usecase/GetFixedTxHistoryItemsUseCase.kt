package com.tangem.domain.txhistory.usecase

import arrow.core.Either
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Returns first page with size [DEFAULT_PAGE_SIZE] of tx history.
 * Page size is preserved to reuse cached data in Token Details Screen.
 *
 * IMPORTANT!!!
 * If page size bigger than [DEFAULT_PAGE_SIZE] is needed consider to implement another use case
 * without use of cached data or increase [DEFAULT_PAGE_SIZE]
 */
class GetFixedTxHistoryItemsUseCase(
    private val repository: TxHistoryRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        refresh: Boolean = false,
    ): Either<TxHistoryListError, Flow<List<TxInfo>>> {
        return Either.catch {
            flow {
                emit(repository.getFixedSizeTxHistoryItems(userWalletId, currency, pageSize, refresh))
            }
        }.mapLeft { TxHistoryListError.DataError(it) }
    }

    suspend fun getSync(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        refresh: Boolean = false,
    ): Either<TxHistoryListError, List<TxInfo>> {
        return Either.catch {
            repository.getFixedSizeTxHistoryItems(userWalletId, currency, pageSize, refresh)
        }.mapLeft { TxHistoryListError.DataError(it) }
    }
}