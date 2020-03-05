package com.tangem.blockchain.xrp

import com.ripple.encodings.addresses.Addresses
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey

class XrpAddressFactory {
    companion object {
        fun makeAddress(walletPublicKey: ByteArray): String {
            val canonicalPublicKey = canonizePublicKey(walletPublicKey)
            val publicKeyHash = canonicalPublicKey.calculateSha256().calculateRipemd160()
            return Addresses.encodeAccountID(publicKeyHash)
        }

        fun canonizePublicKey(publicKey: ByteArray): ByteArray {
            val compressedPublicKey = publicKey.toCompressedPublicKey()
            return if (compressedPublicKey.size == 32) {
                byteArrayOf(0xED.toByte()) + compressedPublicKey
            } else {
                compressedPublicKey
            }
        }
    }
}

class XrpAddressValidator {
    companion object {
        fun validate(address: String): Boolean {
            return try {
                Addresses.decodeAccountID(address)
                address.startsWith("r")
            } catch (excpetion: Exception) {
                false
            }
        }
    }
}