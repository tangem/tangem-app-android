package com.tangem.wallet.xrp

import com.ripple.encodings.addresses.Addresses
import com.ripple.encodings.base58.B58
import org.kethereum.extensions.toBigInteger

class XrpXAddressService {
    companion object {
        private val xrpBase58 = B58("rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz")
        private val xrpMainnetPrefix = byteArrayOf(0x05, 0x44)
        private val zeroTagBytes = ByteArray(4) { 0 }

        fun validate(address: String): Boolean {
            return decode(address) != null
        }

        fun decode(address: String): XrpXAddressDecoded? {
            try {
                val addressBytes = xrpBase58.decodeChecked(address)
                if (addressBytes.size != 31) return null

                val prefix = addressBytes.slice(0..1).toByteArray()
                if (!prefix.contentEquals(xrpMainnetPrefix)) return null

                val accountBytes = addressBytes.slice(2..21).toByteArray()
                val classicAddress = Addresses.encodeAccountID(accountBytes)

                val flag = addressBytes[22]

                val tagBytes = addressBytes.slice(23..26).toByteArray()
                val reservedTagBytes = addressBytes.slice(27..30).toByteArray()
                if (!reservedTagBytes.contentEquals(zeroTagBytes)) return null

                var tag: Int? = null
                when (flag) {
                    0.toByte() -> if (!tagBytes.contentEquals(zeroTagBytes)) return null
                    1.toByte() -> {
                        tag = tagBytes.reversedArray().toBigInteger().toInt()
                    }
                    else -> return null
                }
                return XrpXAddressDecoded(classicAddress, tag)
            } catch (e: Exception) {
                return null
            }
        }
    }
}

data class XrpXAddressDecoded(
        val address: String,
        val destinationTag: Int?
)