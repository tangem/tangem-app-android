package com.tangem.data

import com.tangem.enums.Status

class ResponseApdu(val data: ByteArray) {

    private val tlvParser = TlvParser()

    fun deserialize(encryptionKey: ByteArray? = null): ResponseApduParsed? {

        if (data.size < 2) return null

        if (data.size == 2) return ResponseApduParsed(parseStatus(data[0], data[1]), emptyList())

        val tlvList = tlvParser.fromBytes(data.copyOf(data.size - 2))
        val sw1 = data[data.size - 2]
        val sw2 = data[data.size -1]
        return ResponseApduParsed(parseStatus(sw1, sw2), tlvList)
    }

    private fun parseStatus(sw1: Byte, sw2: Byte): Status {
        val code = (0x00FF and sw1.toInt()) shl 8 or (0x00FF and sw2.toInt())
        return Status.byCode(code)
    }

    private fun decrypt(encryptionKey: ByteArray) {

    }

}

data class ResponseApduParsed(val status: Status, val tlvList: List<Tlv>? = null, val parsingError: String? = null) {
    fun statusCompleted(): Boolean = status == Status.ProcessCompleted
}