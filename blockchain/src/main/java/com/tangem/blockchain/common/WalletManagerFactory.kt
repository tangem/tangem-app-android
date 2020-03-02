package com.tangem.blockchain.common

import com.tangem.blockchain.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.cardano.CardanoWalletManager
import com.tangem.blockchain.ethereum.Chain
import com.tangem.blockchain.ethereum.EthereumWalletManager

import com.tangem.blockchain.stellar.StellarWalletManager
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.blockchain.xrp.XrpWalletManager
import com.tangem.commands.Card

object WalletManagerFactory {

    fun makeWalletManager(card: Card): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchainName: String = card.cardData?.blockchainName ?: return null
        val blockchain = Blockchain.fromId(blockchainName)

        val token = getToken(card)
        val wallet = CurrencyWallet.newInstance(blockchain, blockchain.makeAddress(walletPublicKey), token)


        when (blockchain) {
            Blockchain.Bitcoin -> {
                return BitcoinWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        wallet = wallet
                )
            }
            Blockchain.BitcoinTestnet -> {
                return BitcoinWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        wallet = wallet,
                        isTestNet = true
                )
            }
            Blockchain.Ethereum -> {
                val chain = Chain.Mainnet
                return EthereumWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        wallet = wallet,
                        chain = chain
                )
            }
            Blockchain.Stellar -> {
                return StellarWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        wallet = wallet
                )
            }
            Blockchain.Cardano -> {
                return CardanoWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        wallet = wallet
                )
            }
            Blockchain.XRP -> {
                return XrpWalletManager(
                        cardId = card.cardId,
                        walletPublicKey = walletPublicKey,
                        wallet = wallet
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