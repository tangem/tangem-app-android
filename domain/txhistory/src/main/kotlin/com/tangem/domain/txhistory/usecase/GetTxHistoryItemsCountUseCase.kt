package com.tangem.domain.txhistory.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.txhistory.error.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class GetTxHistoryItemsCountUseCase(
    private val repository: TxHistoryRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(networkId: String, derivationPath: String): Either<TxHistoryStateError, Int> {
        return withContext(dispatchers.io) {
            either {
                catch(
                    block = { repository.getTxHistoryItemsCount(networkId, derivationPath) },
                    catch = { e ->
                        raise(
                            when (e) {
                                is TxHistoryStateError.TxHistoryNotImplemented -> e
                                is TxHistoryStateError.EmptyTxHistories -> e
                                else -> TxHistoryStateError.DataError(e)
                            }
                        )
                    },
                )
            }
        }
    }
}
