package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.AddressService
import org.stellar.sdk.KeyPair

class StellarAddressService: AddressService {
    override fun makeAddress(walletPublicKey: ByteArray): String {
        val kp = KeyPair.fromPublicKey(walletPublicKey)
        return kp.accountId
    }

    override fun validate(address: String): Boolean {
        return try {
            KeyPair.fromAccountId(address) != null
        } catch (exception: IllegalArgumentException) {
            false
        }
    }
}