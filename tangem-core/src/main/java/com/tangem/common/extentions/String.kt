package com.tangem.common.extentions

import java.nio.charset.Charset
import java.security.MessageDigest


fun String.calculateSha256(): ByteArray {
    val sha256 = MessageDigest.getInstance("SHA-256")
    val data = this.toByteArray(Charset.forName("UTF-8"))
    return sha256.digest(data)
}

fun String.hexToBytes(): ByteArray {
    val bytes = ByteArray(this.length / 2)
    for (i in bytes.indices) {
        bytes[i] = Integer.parseInt(this.substring(2 * i, 2 * i + 2),
                16).toByte()
    }
    return bytes
}