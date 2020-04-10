package com.tangem.common.apdu

import com.tangem.EncryptionMode
import com.tangem.common.extensions.calculateCrc16
import com.tangem.common.extensions.toByteArray
import com.tangem.crypto.encrypt
import java.io.ByteArrayOutputStream

/**
 * Class that provides conversion of serialized request and Instruction code
 * to a raw data that can be sent to the card.
 *
 * @property ins Instruction code that determines the type of request for the card.
 * @property tlvs Tlvs encoded to a [ByteArray] that are to be sent to the card.
 */
class CommandApdu(

        private val ins: Int,
        private val tlvs: ByteArray,

        private val le: Int = 0x00,

        private val encryptionMode: EncryptionMode = EncryptionMode.NONE,
        private val encryptionKey: ByteArray? = null,

        private val cla: Int = ISO_CLA) {

    constructor(
            instruction: Instruction,
            tlvs: ByteArray,
            encryptionMode: EncryptionMode = EncryptionMode.NONE,
            encryptionKey: ByteArray? = null
    ) : this(
            instruction.code,
            tlvs,
            encryptionMode = encryptionMode,
            encryptionKey = encryptionKey
    )

    private val p1: Int
    private val p2: Int

    init {
        if (ins == Instruction.OpenSession.code) {
            p1 = 0x00
            p2 = encryptionMode.code.toInt()
        } else {
            p1 = encryptionMode.code.toInt()
            p2 = 0x00
        }
    }


    /**
     * Request converted to a raw data
     */
    val apduData: ByteArray

    init {
        apduData = toBytes()
    }


    private fun toBytes(): ByteArray {
        val data = if (encryptionKey != null) tlvs.encrypt() else tlvs

        val byteStream = ByteArrayOutputStream()
        byteStream.write(cla)
        byteStream.write(ins)
        byteStream.write(p1)
        byteStream.write(p2)
        if (data.isNotEmpty()) {
            byteStream.writeLength(data.size)
            byteStream.write(data)
        }
        return byteStream.toByteArray()
    }

    private fun ByteArrayOutputStream.writeLength(lc: Int) {
        this.write(0)
        this.write(lc shr 8)
        this.write(lc and 0xFF)
    }


    private fun ByteArray.encrypt(): ByteArray {
            val crc: ByteArray = tlvs.calculateCrc16()
            val stream = ByteArrayOutputStream()
            stream.write(this.size.toByteArray(2))
            stream.write(crc)
            stream.write(this)
            return stream.toByteArray().encrypt(encryptionKey!!)
        }

    companion object {
        const val ISO_CLA = 0x00
    }
}