package com.tangem.blockchain.common

import com.tangem.blockchain.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.eth.EthereumWalletManager
import com.tangem.blockchain.stellar.StellarWalletManager
import com.tangem.commands.Card

object WalletManagerFactory {

    fun makeWalletManager(card: Card): WalletManager? {
        val walletPublicKey = card.walletPublicKey ?: return null
        val blockchainName = card.cardData?.blockchainName ?: return null

        when {
            blockchainName.contains("btc") || blockchainName.contains("bitcoin") -> {
                return BitcoinWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true),
                        isTestNet = blockchainName.contains("test"))
            }
            blockchainName.contains("eth") -> {
                return EthereumWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true))
            }
            blockchainName.contains("xlm") -> {
                val token = getToken(card)
                return StellarWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, token == null),
                        token = token,
                        isTestNet = blockchainName.contains("test"))
            }
            else -> return null
        }
    }

    private fun getToken(card: Card): Token? {
        val symbol = card.cardData?.tokenSymbol ?: return null
        val contractAddress = card.cardData?.tokenContractAddress ?: return null
        val decimals = card.cardData?.tokenDecimal ?: return null
        return Token(symbol, contractAddress, decimals)
    }
}


data class Token(
        val symbol: String,
        val contractAddress: String,
        val decimals: Int
)