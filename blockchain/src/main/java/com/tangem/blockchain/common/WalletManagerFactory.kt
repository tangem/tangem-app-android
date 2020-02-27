package com.tangem.blockchain.common

import com.tangem.blockchain.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.ethereum.Chain
import com.tangem.blockchain.ethereum.EthereumWalletManager
import com.tangem.blockchain.cardano.CardanoWalletManager
import com.tangem.blockchain.stellar.StellarWalletManager
import com.tangem.blockchain.xrp.XrpWalletManager
import com.tangem.commands.Card

object WalletManagerFactory {

    fun makeWalletManager(card: Card): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchainName: String = card.cardData?.blockchainName ?: return null

        when {
            blockchainName == Blockchain.Bitcoin.id -> {
                return BitcoinWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true)
                )
            }
            blockchainName == Blockchain.BitcoinTestnet.id -> {
                return BitcoinWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true),
                        isTestNet = true
                )
            }
            blockchainName == Blockchain.Ethereum.id -> {
                val chain = Chain.Mainnet
                return EthereumWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true),
                        chain = chain
                )
            }
            blockchainName == Blockchain.Stellar.id -> {
                val token = getToken(card)
                return StellarWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, token == null),
                        token = token
                )
            }
            blockchainName == Blockchain.Cardano.id -> {
                return CardanoWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(false, true)
                )
            }
            blockchainName == Blockchain.XRP.id -> {
                return XrpWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        walletConfig = WalletConfig(true, true)
                )
            }
            else -> return null
        }
    }

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