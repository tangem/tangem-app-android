package com.tangem.common.apdu

import com.tangem.common.tlv.Tlv
import com.tangem.enums.StatusWord

class ResponseApdu(val data: ByteArray) {

    private val sw1: Int = 0x00FF and data[data.size - 2].toInt()
    private val sw2: Int = 0x00FF and data[data.size - 1].toInt()

    val sw: Int = sw1 shl 8 or sw2

    val statusWord: StatusWord = StatusWord.byCode(sw)

    fun getTlvData(encryptionKey: ByteArray? = null): List<Tlv>? {
        val tlvs = when {
            data.size < 2 -> null
            data.size == 2 -> emptyList()
            else -> Tlv.fromBytes(data.copyOf(data.size - 2))
        }
        return flattenNestedTlvs(tlvs)
    }

    private fun flattenNestedTlvs(tlvs: List<Tlv>?): List<Tlv>? =
            tlvs?.flatMap {
                if (it.tag.hasNestedTlv()) {
                    Tlv.fromBytes(it.value)
                } else {
                    listOf(it)
                }
            }

    private fun decrypt(encryptionKey: ByteArray) {

    }

}