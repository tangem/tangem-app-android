package com.tangem.domain.transaction

import arrow.core.Either
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSignedTransaction
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSimulation
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SolanaGaslessError

interface SolanaGaslessTransactionRepository {
    suspend fun getFeePayer(): Either<SolanaGaslessError, String>
    suspend fun submit(
        userWalletId: UserWalletId,
        transaction: ByteArray,
        lastValidBlockHeight: Long,
    ): Either<SolanaGaslessError, String>

    /**
     * Signs a provider-built Solana transaction for the gasless [feePayer] (account index 0), without
     * broadcasting. With [sponsorsRent] the fee payer also pays the rent of the wallet's own new token accounts.
     * Returns `null` if the wallet manager is unavailable or is not Solana.
     */
    @Suppress("LongParameterList")
    suspend fun sign(
        userWalletId: UserWalletId,
        network: Network,
        transactionData: TransactionData,
        feePayer: String,
        signer: TransactionSigner,
        sponsorsRent: Boolean,
    ): Result<SolanaGaslessSignedTransaction>?

    /**
     * Simulates a provider-built Solana transaction as if [feePayer] paid for it, without signing. With [sponsorsRent]
     * the fee payer also pays the rent of the wallet's own new token accounts. Returns `null` if the wallet manager is
     * unavailable or is not Solana.
     */
    suspend fun simulate(
        userWalletId: UserWalletId,
        network: Network,
        transactionData: TransactionData,
        feePayer: String,
        sponsorsRent: Boolean,
    ): Result<SolanaGaslessSimulation>?
}