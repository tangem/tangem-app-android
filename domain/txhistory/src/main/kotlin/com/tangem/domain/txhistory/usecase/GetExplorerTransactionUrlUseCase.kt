package com.tangem.domain.txhistory.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.txhistory.models.TxStatusError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import javax.inject.Inject

class GetExplorerTransactionUrlUseCase @Inject constructor(
    private val repository: TxHistoryRepository,
) {
    operator fun invoke(
        txHash: String,
        networkId: Network.ID,
        currency: CryptoCurrency,
    ): Either<TxStatusError, String> {
        return either {
            catch(
                block = {
                    if (txHash.isEmpty()) {
                        raise(TxStatusError.EmptyUrlError)
                    }

                    repository.getTxExploreUrl(txHash, networkId, currency).ifEmpty {
                        raise(TxStatusError.EmptyUrlError)
                    }
                },
                catch = { raise(TxStatusError.DataError(it)) },
            )
        }
    }
}