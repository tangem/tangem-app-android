package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData

interface BlockAidRepository {

    suspend fun verifyDAppDomain(data: DAppData): CheckDAppResult

    suspend fun verifyTransaction(data: TransactionData): CheckTransactionResult
}