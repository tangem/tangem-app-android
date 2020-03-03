package com.tangem.blockchain.common

import com.tangem.blockchain.bitcoin.BitcoinAddressFactory
import com.tangem.blockchain.bitcoin.BitcoinAddressValidator
import com.tangem.blockchain.cardano.CardanoAddressFactory
import com.tangem.blockchain.cardano.CardanoAddressValidator
import com.tangem.blockchain.ethereum.EthereumAddressFactory
import com.tangem.blockchain.ethereum.EthereumAddressValidator

import com.tangem.blockchain.stellar.StellarAddressFactory
import com.tangem.blockchain.xrp.XrpAddressFactory
import com.tangem.blockchain.xrp.XrpAddressValidator

import java.math.BigDecimal

enum class Blockchain(
        val id: String,
        val currency: String,
        val decimals: Byte,
        val fullName: String,
        val pendingTransactionTimeout: Int
) {
    Unknown("", "", 0, "", 0),
    Bitcoin("BTC", "BTC", 8, "Bitcoin", 0),
    BitcoinTestnet("BTC", "BTC", 8, "Bitcoin Testnet", 0),
    Ethereum("ETH", "ETH", 18, "Ethereum", 0),
    Rootstock("", "", 18, "", 0),
    Cardano("CARDANO", "ADA", 6, "Cardano", 0),
    XRP("", "XRP", 6, "XRP Ledger", 0),
    Binance("", "", 8, "", 0),
    Stellar("XLM", "XLM", 7, "Stellar", 0);

    fun roundingMode(): Int = when (this) {
        Bitcoin, Ethereum, Rootstock, Binance -> BigDecimal.ROUND_DOWN
        Cardano -> BigDecimal.ROUND_UP
        else -> BigDecimal.ROUND_HALF_UP
    }

    fun makeAddress(walletPublicKey: ByteArray): String {
        return when (this) {
            Unknown -> throw Exception("unsupported blockchain")
            Bitcoin -> BitcoinAddressFactory.makeAddress(walletPublicKey)
            BitcoinTestnet -> BitcoinAddressFactory.makeAddress(walletPublicKey, testNet = true)
            Ethereum -> EthereumAddressFactory.makeAddress(walletPublicKey)
//            Rootstock -> RootstockAddressFactory.makeAddress(cardPublicKey)
            Cardano -> CardanoAddressFactory.makeAddress(walletPublicKey)
            XRP -> XrpAddressFactory.makeAddress(walletPublicKey)
//            Binance -> BinanceAddressFactory.makeAddress(cardPublicKey)
            Stellar -> StellarAddressFactory.makeAddress(walletPublicKey)
            else -> throw Exception("unsupported blockchain")
        }
    }

    fun validateAddress(address: String): Boolean {
        return when (this) {
            Unknown -> throw Exception("unsupported blockchain")
            Bitcoin -> BitcoinAddressValidator.validate(address)
            BitcoinTestnet -> BitcoinAddressValidator.validate(address, testNet = true)
            Ethereum -> EthereumAddressValidator.validate(address)
//            Rootstock -> RootstockAddressValidator.validate(address)
            Cardano -> CardanoAddressValidator.validate(address)
            XRP -> XrpAddressValidator.validate(address)
//            Binance -> BinanceAddressValidator.validate(address)
//            Stellar -> StellarAddressValidator.validate(address)
            else -> throw Exception("unsupported blockchain")
        }
    }

    companion object {
        private val values = values()
        fun fromId(id: String): Blockchain = values.find { it.id == id } ?: Unknown
        fun fromName(name: String): Blockchain = values.find { it.name == name } ?: Unknown
        fun fromCurrency(currency: String): Blockchain = values.find { it.currency == currency }
                ?: Unknown
    }
}
