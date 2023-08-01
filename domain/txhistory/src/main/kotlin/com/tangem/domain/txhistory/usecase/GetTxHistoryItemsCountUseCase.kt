package com.tangem.domain.txhistory.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.txhistory.error.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepository

class GetTxHistoryItemsCountUseCase(private val repository: TxHistoryRepository) {

    suspend operator fun invoke(networkId: String, derivationPath: String): Either<TxHistoryStateError, Int> {
        return either {
            catch(
                block = { repository.getTxHistoryItemsCount(networkId, derivationPath) },
                catch = { throwable ->
                    raise(
                        when (throwable) {
                            is TxHistoryStateError.TxHistoryNotImplemented -> throwable
                            is TxHistoryStateError.EmptyTxHistories -> throwable
                            else -> TxHistoryStateError.DataError(throwable)
                        },
                    )
                },
            )
        }
    }
}