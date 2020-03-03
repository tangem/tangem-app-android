package com.tangem.blockchain.wallets

import com.tangem.blockchain.common.*
import java.util.*

class CurrencyWallet(
        val blockchain: Blockchain,
        address: String,
        override val config: WalletConfig
) : Wallet, TransactionValidator {

    override val exploreUrl: String? = null
    override val shareUrl: String? = null
    override val address: String
        get() = balances[AmountType.Coin]!!.address!!

    val pendingTransactions: MutableList<TransactionData> = mutableListOf()
    val balances: MutableMap<AmountType, Amount> = mutableMapOf()

    init {
        addAmount(Amount(null, blockchain, address))
    }

    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<ValidationError> {
        TODO("not implemented")
    }

    fun addAmount(amount: Amount) {
        balances[amount.type] = amount
    }

    fun addPendingTransaction(transaction: TransactionData) {
        pendingTransactions.add(transaction.copy(date = Calendar.getInstance()))
    }

    companion object {
        fun newInstance(blockchain: Blockchain, address: String, token: Token?): CurrencyWallet {

            return when (blockchain) {
                Blockchain.Bitcoin, Blockchain.XRP -> {
                    val config = WalletConfig(true, true)
                    CurrencyWallet(blockchain, address, config)
                }
                Blockchain.Cardano -> {
                    val config = WalletConfig(false, true)
                    CurrencyWallet(blockchain, address, config)
                }
                Blockchain.Ethereum -> {
                    val config = WalletConfig(true, token == null)
                    val wallet = CurrencyWallet(blockchain, address, config)
                    if (token != null) wallet.addAmount(Amount(token))
                    wallet
                }
                Blockchain.Stellar -> {
                    val config = WalletConfig(false, token == null)
                    val wallet = CurrencyWallet(blockchain, address, config)
                    if (token != null) wallet.addAmount(Amount(token))
                    wallet.addAmount(Amount(null, blockchain, address, AmountType.Reserve))
                    wallet
                }
                else -> throw Exception("Unsupported blockchain")
            }

        }
    }
}