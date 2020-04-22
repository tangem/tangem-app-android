package com.tangem.blockchain.blockchains.binance

import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.common.AddressService
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.Bech32

class BinanceAddressService(private val testNet: Boolean = false) : AddressService {
    override fun makeAddress(walletPublicKey: ByteArray): String {
        val publicKeyHash = walletPublicKey.toCompressedPublicKey().calculateSha256().calculateRipemd160()
        return if (testNet) {
            Bech32.encode("tbnb", Crypto.convertBits(publicKeyHash, 0,
                    publicKeyHash.size, 8, 5, false)
            )
        } else {
            Bech32.encode("bnb", Crypto.convertBits(publicKeyHash, 0,
                    publicKeyHash.size, 8, 5, false)
            )
        }
    }

    override fun validate(address: String): Boolean {
        return try {
            Crypto.decodeAddress(address)
            if (testNet) {
                address.startsWith("tbnb1")
            } else {
                address.startsWith("bnb1")
            }
        } catch (exception: Exception) {
            false
        }
    }
}

enum class BinanceChain(val value: String) {
    Nile("Binance-Chain-Nile"),
    Tigris("Binance-Chain-Tigris");

    companion object {
        fun getChain(testNet: Boolean): BinanceChain = if (testNet) Nile else Tigris
    }
}