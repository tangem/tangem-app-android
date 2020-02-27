package com.tangem.blockchain.bitcoin


import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.Base58
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import java.security.MessageDigest

class BitcoinAddressFactory {
    companion object {
        fun makeAddress(walletPublicKey: ByteArray, testNet: Boolean = false): String {
            val netSelectionByte = if (testNet) 0x6f.toByte() else 0x00.toByte()
            val hash1 = walletPublicKey.calculateSha256().calculateRipemd160()
            val hash2 = byteArrayOf(netSelectionByte).plus(hash1).calculateSha256().calculateSha256()
            val result = byteArrayOf(netSelectionByte) + hash1 + hash2[0] + hash2[1] + hash2[2] + hash2[3]
            return Base58.encode(result)
        }
    }
}

class BitcoinAddressValidator {
    companion object {

        private const val firstLetters = "123nm"
        private const val firstLettersNonTestNet = "13"

        fun validate(address: String, testNet: Boolean = false): Boolean {
            if (firstLetters.contains(address.first())) {
                if (testNet && firstLettersNonTestNet.contains(address.first())) return false
                if (address.length !in 26..35) return false
                val decoded = address.decodeBase58() ?: return false
                val hash = recursiveSha256(decoded, 0, 21, 2)
                return hash.sliceArray(0..3).contentEquals(decoded.sliceArray(21..24))
            } else {
                return validateSegwitAddress(address, testNet)
            }
        }

        private fun recursiveSha256(data: ByteArray, start: Int, len: Int, recursion: Int): ByteArray {
            if (recursion == 0) return data
            val md = MessageDigest.getInstance("SHA-256")
            md.update(data.sliceArray(start until start + len))
            return recursiveSha256(md.digest(), 0, 32, recursion - 1)
        }

        private fun String.decodeBase58(): ByteArray? {
            return try {
                Base58.decode(this)
            } catch (exception: AddressFormatException) {
                null
            }
        }

        private fun validateSegwitAddress(address: String, testNet: Boolean): Boolean {
            return try {
                if (testNet) {
                    SegwitAddress.fromBech32(TestNet3Params(), address)
                    true
                } else {
                    SegwitAddress.fromBech32(MainNetParams(), address)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}