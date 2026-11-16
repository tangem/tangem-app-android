package com.tangem.domain.transaction

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SolanaGaslessError

interface SolanaGaslessTransactionRepository {
    suspend fun getFeePayer(): Either<SolanaGaslessError, String>
    suspend fun submit(
        userWalletId: UserWalletId,
        transaction: ByteArray,
        lastValidBlockHeight: Long,
    ): Either<SolanaGaslessError, String>
}