package com.tangem.common.extensions

import java.nio.ByteBuffer

fun Int.toByteArray(): ByteArray {
    val bufferSize = Int.SIZE_BYTES
    val buffer = ByteBuffer.allocate(bufferSize)
    buffer.putInt(this)
    return buffer.array()
}