package com.tangem.blockchain.common

interface AddressService {
    fun makeAddress(walletPublicKey: ByteArray): String
    fun validate(address: String): Boolean
}