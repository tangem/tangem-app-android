package com.tangem.domain.blockaid

import arrow.core.Either
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData

/**
 * Verifies the safety of DApps and WalletConnect transactions
 */
interface BlockAidVerifier {

    /**
     * Checks if a DApp is safe to use
     */
    suspend fun verifyDApp(data: DAppData): Either<Throwable, CheckDAppResult>

    /**
     * Checks the safety of a WalletConnect transaction and provides a simulation result
     */
    suspend fun verifyTransaction(data: TransactionData): Either<Throwable, CheckTransactionResult>
}