package com.tangem.domain.txhistory.usecase

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.txhistory.repository.TxHistoryRepository

class GetExplorerTransactionUrlUseCase(
    private val repository: TxHistoryRepository,
) {
    operator fun invoke(txHash: String, networkId: Network.ID): String {
        return repository.getTxExploreUrl(txHash, networkId)
    }
}