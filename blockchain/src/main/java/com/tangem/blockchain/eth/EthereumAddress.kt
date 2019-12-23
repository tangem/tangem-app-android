package com.tangem.blockchain.eth

import org.kethereum.crypto.toAddress
import org.kethereum.functions.isValid
import org.kethereum.model.Address
import org.kethereum.model.PublicKey
import org.kethereum.wallet.model.WalletCrypto

object EthereumAddressFactory {
    fun makeAddress(cardPublicKey: ByteArray, testNet: Boolean = false): String =
            PublicKey(cardPublicKey).toAddress().hex
}

object EthereumAddressValidator {
    fun validate(address: String, testNet: Boolean = false): Boolean = Address(address).isValid()
}