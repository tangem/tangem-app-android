package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService

enum class Blockchain(
        val id: String,
        val currency: String,
        val fullName: String
) {
    Unknown("", "", ""),
    Bitcoin("BTC", "BTC", "Bitcoin"),
    BitcoinTestnet("BTC/test", "BTCt", "Bitcoin Testnet"),
    BitcoinCash("BCH", "BCH", "Bitcoin Cash"),
    Ethereum("ETH", "ETH", "Ethereum"),
    RSK("RSK", "RBTC", "RSK"),
    Cardano("CARDANO", "ADA", "Cardano"),
    XRP("XRP", "XRP", "XRP Ledger"),
    Binance("BINANCE", "BNB", "Binance"),
    BinanceTestnet("BINANCE/test", "BNBt", "Binance"),
    Stellar("XLM", "XLM", "Stellar");

    fun decimals(): Int = when (this) {
        Bitcoin, BitcoinTestnet, BitcoinCash, Binance, BinanceTestnet -> 8
        Cardano, XRP -> 6
        Ethereum, RSK -> 18
        Stellar -> 7
        Unknown -> 0
    }

    fun pendingTransactionsTimeout(): Int = when (this) {
        else -> 0
    }

    fun makeAddress(walletPublicKey: ByteArray): String = getAddressService().makeAddress(walletPublicKey)

    fun validateAddress(address: String): Boolean = getAddressService().validate(address)

    private fun getAddressService(): AddressService = when (this) {
        Unknown -> throw Exception("unsupported blockchain")
        Bitcoin -> BitcoinAddressService()
        BitcoinTestnet -> BitcoinAddressService(true)
        BitcoinCash -> BitcoinCashAddressService()
        Ethereum -> EthereumAddressService()
        RSK -> throw Exception("unsupported blockchain")
        Cardano -> CardanoAddressService()
        XRP -> XrpAddressService()
        Binance -> BinanceAddressService()
        BinanceTestnet -> BinanceAddressService(true)
        Stellar -> StellarAddressService()
    }

    fun getShareUri(address: String): String = when (this) {
        Bitcoin -> "bitcoin:$address"
        Ethereum -> "ethereum:$address"
        XRP -> "ripple:$address"
        else -> address
    }

    fun getExploreUrl(address: String, token: Token? = null): String = when (this) {
        Binance -> "https://explorer.binance.org/address/$address"
        Bitcoin -> "https://blockchain.info/address/$address"
        BitcoinTestnet -> "https://live.blockcypher.com/btc-testnet/address/$address"
        BitcoinCash -> "https://blockchair.com/bitcoin-cash/address/$address"
        Cardano -> "https://cardanoexplorer.com/address/$address"
        Ethereum -> if (token == null) {
            "https://etherscan.io/address/"
        } else {
            "https://etherscan.io/token/${token.contractAddress}?a=$address"
        }
        RSK -> {
            var url = "https://explorer.rsk.co/address/$address"
            if (token != null) {
                url += "?__tab=tokens"
            }
            url
        }
        Stellar -> "https://stellar.expert/explorer/public/account/$address"
        XRP -> "https://xrpscan.com/account/$address"
        else -> throw Exception("Explore URL not defined!")
    }

    companion object {
        private val values = values()
        fun fromId(id: String): Blockchain = values.find { it.id == id } ?: Unknown
        fun fromName(name: String): Blockchain = values.find { it.name == name } ?: Unknown
        fun fromCurrency(currency: String): Blockchain = values.find { it.currency == currency }
                ?: Unknown
    }
}
