package com.tangem.common.extensions

import java.nio.charset.Charset
import java.security.MessageDigest

/**
 * Extension functions for [String].
 */
fun String.calculateSha256(): ByteArray {
    val sha256 = MessageDigest.getInstance("SHA-256")
    val data = this.toByteArray(Charset.forName("UTF-8"))
    return sha256.digest(data)
}

fun String.calculateSha512(): ByteArray {
    val sha = MessageDigest.getInstance("SHA-512")
    val data = this.toByteArray(Charset.forName("UTF-8"))
    return sha.digest(data)
}

fun String.hexToBytes(): ByteArray {
    return ByteArray(this.length / 2)
    { i ->
        Integer.parseInt(this.substring(2 * i, 2 * i + 2), 16).toByte()
    }
}