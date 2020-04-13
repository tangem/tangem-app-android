package com.tangem.common.tlv

import com.tangem.Log

class TlvBuilder {
    private val tlvs = mutableListOf<Tlv>()
    private val encoder = TlvEncoder()

    internal inline fun <reified T> append(tag: TlvTag, value: T?) {
        if (value == null) return

        tlvs.add(encoder.encode(tag, value))
    }

    fun serialize(): ByteArray {
        Log.v("TLV",
                "List of TLV that are being sent to a card:\n${tlvs.joinToString("\n")}")
        return tlvs.serialize()
    }

}