package com.tangem.common.apdu

import com.tangem.EncryptionMode
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.toBytes
import java.io.ByteArrayOutputStream

class CommandApdu(

        private val instruction: Int,
        private val tlvList: List<Tlv>,

        private val cla: Byte = ISO_CLA,
        private val p1: Byte = 0x00,
        private val p2: Byte = 0x00,

        private val le: Int = 0x00,

        private val encryptionMode: EncryptionMode = EncryptionMode.NONE,
        private val encryptionKey: ByteArray? = null) {

    val apduData: ByteArray

    init {
        apduData = toBytes()
    }


    private fun toBytes(): ByteArray {

        val data = if (tlvList.isNotEmpty()) {
            tlvList.toBytes()
        } else {
            byteArrayOf()
        }

        val lc = data.size


        val byteStream = ByteArrayOutputStream()
        byteStream.write(cla.toInt())
        byteStream.write(instruction)
        byteStream.write(p1.toInt())
        byteStream.write(p2.toInt())
        if (lc != 0) {
            writeLength(byteStream, lc)
            byteStream.write(data)
        }
        return byteStream.toByteArray()
    }

    private fun writeLength(stream: ByteArrayOutputStream, lc: Int) {

        stream.write(0)
        stream.write(lc shr 8)
        stream.write(lc and 0xFF)
    }


    private fun encrypt() {

    }

    companion object {
        const val ISO_CLA = 0x00.toByte()
    }
}