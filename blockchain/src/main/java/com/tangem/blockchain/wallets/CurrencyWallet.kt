package com.tangem.blockchain.wallets

import com.tangem.blockchain.common.*
import java.util.*

class CurrencyWallet(
        override val config: WalletConfig,
        override val address: String,
        override val exploreUrl: String? = null,
        override val shareUrl: String? = null,
        val pendingTransactions: List<TransactionData> = listOf(),
        val balances: MutableList<Amount> = mutableListOf(),
        val isTestnet: Boolean = false
) : Wallet, TransactionValidator {


    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<ValidationError> {
        TODO("not implemented")
    }
}