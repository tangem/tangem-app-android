package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkManager
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkManager
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkManager
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkManager
import com.tangem.blockchain.blockchains.stellar.StellarNetworkManager
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder

import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkManager
import com.tangem.commands.Card

object WalletManagerFactory {

    fun makeWalletManager(card: Card): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchainName: String = card.cardData?.blockchainName ?: return null
        val blockchain = Blockchain.fromId(blockchainName)

        val cardId = card.cardId

        val token = getToken(card)

        val wallet = Wallet(blockchain, blockchain.makeAddress(walletPublicKey), token)

        when (blockchain) {
            Blockchain.Bitcoin -> {
                return BitcoinWalletManager(
                        card.cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey),
                        BitcoinNetworkManager()
                )
            }
            Blockchain.BitcoinTestnet -> {
                return BitcoinWalletManager(
                        card.cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, true),
                        BitcoinNetworkManager(true)
                )
            }
            Blockchain.Ethereum -> {
                val chain = Chain.Mainnet
                return EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, chain),
                        EthereumNetworkManager()
                )
            }
            Blockchain.Stellar -> {
                val networkManager = StellarNetworkManager()

                return StellarWalletManager(
                        cardId, wallet,
                        StellarTransactionBuilder(networkManager, walletPublicKey),
                        networkManager
                )
            }
            Blockchain.Cardano -> {
                return CardanoWalletManager(
                        cardId, wallet,
                        CardanoTransactionBuilder(walletPublicKey),
                        CardanoNetworkManager()
                )
            }
            Blockchain.XRP -> {
                return XrpWalletManager(
                        cardId, wallet,
                        XrpTransactionBuilder(walletPublicKey),
                        XrpNetworkManager()
                )
            }
            Blockchain.Binance -> {
                return BinanceWalletManager(
                        cardId, wallet,
                        BinanceTransactionBuilder(walletPublicKey),
                        BinanceNetworkManager()
                )
            }
            Blockchain.BinanceTestnet -> {
                return BinanceWalletManager(
                        cardId, wallet,
                        BinanceTransactionBuilder(walletPublicKey, true),
                        BinanceNetworkManager(true)
                )
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