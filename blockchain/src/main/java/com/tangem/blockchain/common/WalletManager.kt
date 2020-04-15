package com.tangem.blockchain.common

import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult

interface WalletManager {
    var wallet: CurrencyWallet
    val blockchain: Blockchain

    suspend fun update()
}

interface TransactionSender {
    suspend fun send(transactionData: TransactionData, signer: TransactionSigner) : SimpleResult
}

interface TransactionSigner {
    suspend fun sign(hashes: Array<ByteArray>, cardId: String): CompletionResult<SignResponse>
}

interface FeeProvider {
    suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>>
}