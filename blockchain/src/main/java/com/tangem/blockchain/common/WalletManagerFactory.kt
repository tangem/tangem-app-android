package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkManager
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkManager
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashNetworkManager
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkManager
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkManager
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkManager
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkManager
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.stellar.StellarNetworkManager
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkManager
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkManager
import com.tangem.commands.Card
import com.tangem.common.extensions.toCompressedPublicKey

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
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        BitcoinNetworkManager(blockchain)
                )
            }
            Blockchain.BitcoinTestnet -> {
                return BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        BitcoinNetworkManager(blockchain)
                )
            }
            Blockchain.BitcoinCash -> {
                return BitcoinCashWalletManager(
                        cardId, wallet,
                        BitcoinCashTransactionBuilder(walletPublicKey.toCompressedPublicKey(), blockchain),
                        BitcoinCashNetworkManager()
                )
            }
            Blockchain.Litecoin -> {
                return LitecoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        LitecoinNetworkManager()
                )
            }
            Blockchain.Ducatus -> {
                return DucatusWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        DucatusNetworkManager()
                )
            }
            Blockchain.Ethereum, Blockchain.RSK -> {
                return EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkManager(blockchain)
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
            Blockchain.Tezos -> {
                return TezosWalletManager(
                        cardId, wallet,
                        TezosTransactionBuilder(walletPublicKey),
                        TezosNetworkManager()
                )
            }
            Blockchain.Unknown -> throw Exception("unsupported blockchain")
        }
    }

    private fun getToken(card: Card): Token? {
        val symbol = card.cardData?.tokenSymbol ?: return null
        val contractAddress = card.cardData?.tokenContractAddress ?: return null
        val decimals = card.cardData?.tokenDecimal ?: return null
        return Token(symbol, contractAddress, decimals)
    }
}