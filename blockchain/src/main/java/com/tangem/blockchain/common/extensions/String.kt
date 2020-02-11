package com.tangem.blockchain.common.extensions

import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.Base58

fun String.decodeBase58(): ByteArray? {
    return try {
        Base58.decode(this)
    } catch (exception: AddressFormatException) {
        null
    }
}