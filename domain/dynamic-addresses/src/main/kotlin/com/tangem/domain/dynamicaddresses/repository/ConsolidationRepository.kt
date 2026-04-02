package com.tangem.domain.dynamicaddresses.repository

import arrow.core.Either
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.domain.dynamicaddresses.model.ConsolidationInfo
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

interface ConsolidationRepository {

    suspend fun getConsolidationInfo(
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, ConsolidationInfo>

    suspend fun sendConsolidationTransaction(
        userWalletId: UserWalletId,
        network: Network,
        signer: TransactionSigner,
    ): Either<Throwable, String>
}