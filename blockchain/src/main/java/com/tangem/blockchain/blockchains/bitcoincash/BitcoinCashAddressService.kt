package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.CashAddr
import com.tangem.blockchain.common.AddressService
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey

class BitcoinCashAddressService() : AddressService {
    override fun makeAddress(walletPublicKey: ByteArray): String {
        val publicKeyHash = walletPublicKey.toCompressedPublicKey().calculateSha256().calculateRipemd160()
        return CashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, publicKeyHash)
    }

    override fun validate(address: String): Boolean {
        return CashAddr.isValidCashAddress(address)
    }

    fun getPublicKeyHash(address: String): ByteArray {
        return CashAddr.decodeCashAddress(address).hash
    }
}