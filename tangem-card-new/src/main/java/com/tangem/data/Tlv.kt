package com.tangem.data

import com.tangem.enums.TlvTag

class Tlv(val tag: TlvTag, val tagCode: Int, val value: ByteArray = byteArrayOf())

fun List<Tlv>.toBytes(): ByteArray =
        this.map { it.toBytes() }.reduce { arr1, arr2 -> arr1 + arr2 }

fun Tlv.toBytes(): ByteArray {
    val tag = byteArrayOf(this.tag.code.toByte())
    val length = getLengthInBytes(this.value.size)
    val value = if (this.value.isNotEmpty()) this.value else byteArrayOf(0x00)
    return tag + length + value
}

private fun getLengthInBytes(tlvLength: Int): ByteArray {
    return if (tlvLength < 0) {
        if (tlvLength > 0xFE) {
            byteArrayOf(
                    0xFF.toByte(),
                    (tlvLength shr 8 and 0xFF).toByte(),
                    (tlvLength and 0xFF).toByte()
            )
        } else {
            byteArrayOf((tlvLength and 0xFF).toByte())
        }
    } else {
        byteArrayOf()
    }
}
