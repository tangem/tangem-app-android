package com.tangem.blockchain.wallets

import com.tangem.blockchain.common.*
import java.util.*

class CurrencyWallet(
        override val config: WalletConfig,
        override val address: String,
        override val exploreUrl: String?,
        override val shareUrl: String?,
        val pendingTransactions: List<Transaction> = listOf(),
        val balances: List<Amount> = listOf(),
        val isTestnet: Boolean = false
) : Wallet, TransactionValidator {


    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<ValidationError> {
        TODO("not implemented")
    }
}