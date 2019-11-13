package com.tangem.common.apdu

import com.tangem.common.tlv.Tlv

class ResponseApdu(val data: ByteArray) {

    private val sw1: Int = 0x00FF and data[data.size - 2].toInt()
    private val sw2: Int = 0x00FF and data[data.size - 1].toInt()

    val sw: Int = sw1 shl 8 or sw2

    val statusWord: StatusWord = StatusWord.byCode(sw)

    fun getTlvData(encryptionKey: ByteArray? = null): List<Tlv>? {
        return when {
            data.size < 2 -> null
            data.size == 2 -> emptyList()
            else -> Tlv.tlvListFromBytes(data.copyOf(data.size - 2))
        }
    }


    private fun decrypt(encryptionKey: ByteArray) {
        TODO("not implemented")
    }

}