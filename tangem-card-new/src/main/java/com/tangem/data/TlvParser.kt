package com.tangem.data

import com.tangem.enums.TlvTag
import java.io.ByteArrayInputStream
import java.io.IOException

class TlvParser {

    fun readFromStream(stream: ByteArrayInputStream): Tlv? {
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
        return Tlv(tag, code, value)
    }

    fun fromBytes(data: ByteArray): List<Tlv> {
        val tlvList = mutableListOf<Tlv>()
        val stream = ByteArrayInputStream(data)
        var tlv: Tlv? = null
        do {
            try {
                tlv = readFromStream(stream)
                if (tlv != null) tlvList.add(tlv)
            } catch (e: IOException) {
                throw IOException("TLVError: " + e.message)
            }

        } while (tlv != null)
        return tlvList
    }


    fun parseTlv(data: ByteArray) {


    }
}