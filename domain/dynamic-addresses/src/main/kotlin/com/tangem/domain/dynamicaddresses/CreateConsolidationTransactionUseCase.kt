package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.dynamicaddresses.repository.ConsolidationRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class CreateConsolidationTransactionUseCase(
    private val consolidationRepository: ConsolidationRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, TransactionData> {
        return consolidationRepository.createConsolidationTransaction(userWalletId, network)
    }
}