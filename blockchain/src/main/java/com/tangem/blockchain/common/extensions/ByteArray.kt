package com.tangem.blockchain.common.extensions

import android.util.Base64
import org.bitcoinj.core.Base58

fun ByteArray.encodeBase58(): String {
    return Base58.encode(this)
}

fun ByteArray.encodeBase64NoWrap(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}