package com.tangem.domain.txhistory.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepository

class GetTxHistoryItemsCountUseCase(private val repository: TxHistoryRepository) {

    suspend operator fun invoke(network: Network): Either<TxHistoryStateError, Int> {
        return either {
            catch(
                block = { repository.getTxHistoryItemsCount(network) },
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
