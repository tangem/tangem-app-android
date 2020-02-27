package com.tangem.blockchain.stellar

import org.stellar.sdk.KeyPair

class StellarAddressFactory {
    companion object {
        fun makeAddress(cardPublicKey: ByteArray): String {
            val kp = KeyPair.fromPublicKey(cardPublicKey)
            return kp.accountId
        }
    }
}

class StellarAddressValidator {
    companion object {
        fun validate(address: String): Boolean {
            return try {
                KeyPair.fromAccountId(address) != null
            } catch (exception: IllegalArgumentException) {
                false
            }
        }
    }
}