package com.tangem.blockchain.common

import com.tangem.blockchain.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.cardano.CardanoWalletManager
import com.tangem.blockchain.eth.Chain
import com.tangem.blockchain.eth.EthereumWalletManager
import com.tangem.blockchain.stellar.StellarWalletManager
import com.tangem.commands.Card

object WalletManagerFactory {

    fun makeWalletManager(card: Card): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchainName: String = card.cardData?.blockchainName ?: return null

        when {
            blockchainName.contains("btc") || blockchainName.contains("bitcoin") -> {
                return BitcoinWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true),
                        isTestNet = isTestNet(blockchainName))
            }
            blockchainName.contains("eth") -> {
                val chain = if (isTestNet(blockchainName)) {
                    Chain.EthereumClassicTestnet
                } else {
                    Chain.EthereumClassicMainnet
                }
                return EthereumWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true),
                        chain = chain)
            }
            blockchainName.contains("xlm") -> {
                val token = getToken(card)
                return StellarWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, token == null),
                        token = token,
                        isTestNet = isTestNet(blockchainName))
            }
            blockchainName.contains("cardano") -> {
                return CardanoWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(false, true)
                )
            }
            else -> return null
        }
    }

    private fun isTestNet(blockchainName: String) = blockchainName.contains("test")

    private fun getToken(card: Card): Token? {
        val symbol = card.cardData?.tokenSymbol ?: return null
        val contractAddress = card.cardData?.tokenContractAddress ?: return null
        val decimals = card.cardData?.tokenDecimal ?: return null
        return Token(symbol, contractAddress, decimals.toByte())
    }
}


data class Token(
        val symbol: String,
        val contractAddress: String,
        val decimals: Byte
)