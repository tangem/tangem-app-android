package com.tangem.common.tlv

import com.tangem.Log
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * The data converted to the Tag Length Value protocol.
 */
class Tlv {

    val tag: TlvTag
    val value: ByteArray
    val tagRaw: Int

    constructor(tagCode: Int, value: ByteArray = byteArrayOf()) {
        this.tag = TlvTag.byCode(tagCode)
        this.tagRaw = tagCode
        this.value = value
    }

    constructor(tag: TlvTag, value: ByteArray = byteArrayOf()) {
        this.tag = tag
        this.tagRaw = tag.code
        this.value = value
    }


    companion object {
        private fun tlvFromBytes(stream: ByteArrayInputStream): Tlv? {
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


        fun tlvListFromBytes(mData: ByteArray): List<Tlv>? {
            val tlvList = mutableListOf<Tlv>()
            val stream = ByteArrayInputStream(mData)
            var tlv: Tlv? = null
            do {
                try {
                    tlv = Tlv.tlvFromBytes(stream)
                    if (tlv != null) tlvList.add(tlv)
                } catch (e: IOException) {
                    Log.e(this::class.java.simpleName,"TLVError: " + e.message)
                    return null
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