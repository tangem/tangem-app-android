package com.tangem.blockchain.common

import com.tangem.blockchain.bitcoin.BitcoinAddressFactory
import com.tangem.blockchain.bitcoin.BitcoinAddressValidator
import java.math.BigDecimal

enum class Blockchain(
        val id: String,
        val currency: String,
        val decimals: Byte,
        val fullName: String,
        val pendingTransactionTimeout: Int) {
    Unknown("", "", 0, "", 0),
    Bitcoin("", "", 8, "", 0),
    BitcoinTestnet("", "", 8, "", 0),
    Ethereum("", "", 18, "", 0),
    Rootstock("", "", 18, "", 0),
    Cardano("", "", 6, "", 0),
    Ripple("", "", 6, "", 0),
    Binance("", "", 8, "", 0),
    Stellar("", "", 7, "", 0);

    fun roundingMode(): Int = when (this) {
        Bitcoin, Ethereum, Rootstock, Binance -> BigDecimal.ROUND_DOWN
        Cardano -> BigDecimal.ROUND_UP
        else -> BigDecimal.ROUND_HALF_UP
    }

    fun makeAddress(cardPublicKey: ByteArray): String {
        return when (this) {
            Unknown -> throw Exception("unsupported blockchain")
            Bitcoin -> BitcoinAddressFactory.makeAddress(cardPublicKey)
            BitcoinTestnet -> BitcoinAddressFactory.makeAddress(cardPublicKey, testNet = true)
//            Ethereum -> EthereumAddressFactory.makeAddress(cardPublicKey)
//            Rootstock -> RootstockAddressFactory.makeAddress(cardPublicKey)
//            Cardano -> CardanoAddressFactory.makeAddress(cardPublicKey)
//            Ripple -> RippleAddressFactory.makeAddress(cardPublicKey)
//            Binance -> BinanceAddressFactory.makeAddress(cardPublicKey)
//            Stellar -> StellarAddressFactory.makeAddress(cardPublicKey)
        }
    }

    fun validateAddress(address: String): Boolean {
        return when (this) {
            Unknown -> throw Exception("unsupported blockchain")
            Bitcoin -> BitcoinAddressValidator.validate(address)
            BitcoinTestnet -> BitcoinAddressValidator.validate(address, testNet = true)
//            Ethereum -> EthereumAddressValidator.validate(address)
//            Rootstock -> RootstockAddressValidator.validate(address)
//            Cardano -> CardanoAddressValidator.validate(address)
//            Ripple -> RippleAddressValidator.validate(address)
//            Binance -> BinanceAddressValidator.validate(address)
//            Stellar -> StellarAddressValidator.validate(address)
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
