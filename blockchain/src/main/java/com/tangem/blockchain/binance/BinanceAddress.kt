package com.tangem.blockchain.binance

import com.tangem.blockchain.binance.client.encoding.Crypto
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.Bech32

class BinanceAddressFactory {
    companion object {
        fun makeAddress(walletPublicKey: ByteArray, testNet: Boolean = false): String {
            val publicKeyHash = walletPublicKey.toCompressedPublicKey().calculateSha256().calculateRipemd160()
            return if (testNet) {
                Bech32.encode("tbnb", Crypto.convertBits(publicKeyHash, 0, publicKeyHash.size, 8, 5, false))
            } else {
                Bech32.encode("bnb", Crypto.convertBits(publicKeyHash, 0, publicKeyHash.size, 8, 5, false))
            }
        }
    }
}

class BinanceAddressValidator {
    companion object {
        fun validate(address: String, testNet: Boolean = false): Boolean {
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
}