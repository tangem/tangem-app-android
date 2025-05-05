package com.tangem.data.blockaid

import arrow.core.Either
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData
import com.tangem.domain.blockaid.BlockAidVerifier
import javax.inject.Inject

/**
 * Verifies the safety of DApps and WalletConnect transactions
 */
class DefaultBlockAidVerifier @Inject constructor(
    private val repository: BlockAidRepository,
) : BlockAidVerifier {

    /**
     * Checks if a DApp is safe to use
     */
    override suspend fun verifyDApp(data: DAppData): Either<Throwable, CheckDAppResult> {
        return Either.catch { repository.verifyDAppDomain(data) }
    }

    /**
     * Checks the safety of a WalletConnect transaction and provides a simulation result
     */
    override suspend fun verifyTransaction(data: TransactionData): Either<Throwable, CheckTransactionResult> {
        return Either.catch { repository.verifyTransaction(data) }
    }
}