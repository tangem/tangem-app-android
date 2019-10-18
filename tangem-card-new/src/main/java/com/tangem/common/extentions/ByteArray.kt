package com.tangem.common.extentions

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and


fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }


fun ByteArray.toInt(): Int {
    return when (this.size) {
        1 -> (this[0] and 0xFF.toByte()).toInt()
        2 -> ByteBuffer.wrap(this).short.toInt()
        4 -> ByteBuffer.wrap(this).int
        else -> throw IllegalArgumentException("Length must be 1,2 or 4. Length = " + this.size)
    }
}

fun ByteArray.toDate(): Date {
    val year = ((this[0] and 0xFF.toByte()).toInt() shl 8) or (this[1] and 0xFF.toByte()).toInt()
    val month = this[2] - 1
    val day = this[3].toInt()
    val cd = Calendar.getInstance()
    cd.set(year, month, day, 0, 0, 0)
    return cd.time
}

fun ByteArray.calculateSha512(): ByteArray = MessageDigest.getInstance("SHA-512").digest(this)

