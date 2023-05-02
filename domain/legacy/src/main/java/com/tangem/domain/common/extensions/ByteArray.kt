package com.tangem.domain.common.extensions

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun ByteArray.calculateHmacSha256(key: ByteArray): ByteArray {
    val mac: Mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(this)
}