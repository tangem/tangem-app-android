package com.tangem.domain.dynamicaddresses.repository

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

interface ConsolidationRepository {

    suspend fun createConsolidationTransaction(
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, TransactionData>
}