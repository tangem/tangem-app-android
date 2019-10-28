package com.tangem.common.tlv

import java.io.ByteArrayInputStream
import java.io.IOException

class Tlv {

    val tag: TlvTag
    val value: ByteArray
    val tagCode: Int

    constructor(tagCode: Int, value: ByteArray = byteArrayOf()) {
        this.tag = TlvTag.byCode(tagCode)
        this.tagCode = tagCode
        this.value = value
    }

    constructor(tag: TlvTag, value: ByteArray = byteArrayOf()) {
        this.tag = tag
        this.tagCode = tag.code
        this.value = value
    }


    companion object {
        fun tlvFromBytes(stream: ByteArrayInputStream): Tlv? {
            val code = stream.read()
            if (code == -1) return null
            var len = stream.read()
            if (len == -1)
                throw IOException("Can't read TLV")
            if (len == 0xFF) {
                val lenH = stream.read()
                if (lenH == -1)
                    throw IOException("Can't read TLV")
                len = stream.read()
                if (len == -1)
                    throw IOException("Can't read TLV")
                len = len or (lenH shl 8)
            }
            val value = ByteArray(len)
            if (len > 0) {
                if (len != stream.read(value)) {
                    throw IOException("Can't read TLV")
                }
            }
            val tag = TlvTag.byCode(code)
            return if (tag == TlvTag.Unknown) Tlv(code, value) else Tlv(tag, value)
        }


        fun fromBytes(mData: ByteArray): List<Tlv> {
            val tlvList = mutableListOf<Tlv>()
            val stream = ByteArrayInputStream(mData)
            var tlv: Tlv? = null
            do {
                try {
                    tlv = Tlv.tlvFromBytes(stream)
                    if (tlv != null) tlvList.add(tlv)
                } catch (e: IOException) {
                    throw TlvMapperException("TLVError: " + e.message)
                }

            } while (tlv != null)
            return tlvList
        }
    }

}

fun List<Tlv>.toBytes(): ByteArray =
        this.map { it.toBytes() }.reduce { arr1, arr2 -> arr1 + arr2 }

fun Tlv.toBytes(): ByteArray {
    val tag = byteArrayOf(this.tag.code.toByte())
    val length = getLengthInBytes(this.value.size)
    val value = if (this.value.isNotEmpty()) this.value else byteArrayOf(0x00)
    return tag + length + value
}

private fun getLengthInBytes(tlvLength: Int): ByteArray {
    return if (tlvLength > 0) {
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
