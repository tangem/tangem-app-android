package com.tangem.common.apdu

import com.tangem.EncryptionMode
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.toBytes
import java.io.ByteArrayOutputStream

/**
 * Class that provides conversion of serialized request and Instruction code
 * to a raw data that can be sent to the card.
 *
 * @property ins Instruction code that determines the type of request for the card.
 * @property tlvList A list of TLVs that are to be sent to the card
 */
class CommandApdu(

        private val ins: Int,
        private val tlvList: List<Tlv>,

        private val cla: Byte = ISO_CLA,
        private val p1: Byte = 0x00,
        private val p2: Byte = 0x00,

        private val le: Int = 0x00,

        private val encryptionMode: EncryptionMode = EncryptionMode.NONE,
        private val encryptionKey: ByteArray? = null) {

    constructor(
            instruction: Instruction,
            tlvList: List<Tlv>,
            encryptionMode: EncryptionMode = EncryptionMode.NONE,
            encryptionKey: ByteArray? = null
    ) : this(
            instruction.code,
            tlvList,
            encryptionMode = encryptionMode,
            encryptionKey = encryptionKey
    )


    /**
     * Request converted to a raw data
     */
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
        byteStream.write(ins)
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
        TODO("not implemented")
    }

    companion object {
        const val ISO_CLA = 0x00.toByte()
    }
}