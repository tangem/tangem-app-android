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


    fun toBytes(): ByteArray {

//        var length = 4 // CLA, INS, P1, P2

        val data = if (tlvList.isNotEmpty()) {
            tlvList.toBytes()
        } else {
            byteArrayOf()
        }

        val lc = data.size
//
//        if (data.isNotEmpty()) {
//            length += 1 // LC
//            if (lc >= 256)
//                length += 2
//            length += data.size // DATA
//        }


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

//        val apdu = ByteArray(length)
//
//        var index = 0
//        apdu[index] = cla
//        index++
//        apdu[index] = instruction
//        index++
//        apdu[index] = p1
//        index++
//        apdu[index] = p2
//        index++
//        if (lc != 0) {
//            if (lc < 256) {
//                apdu[index] = lc.toByte()
//                index++
//            } else {
//                apdu[index] = 0
//                index++
//                apdu[index] = (lc shr 8).toByte()
//                index++
//                apdu[index] = (lc and 0xFF).toByte()
//                index++
//            }
//
//            System.arraycopy(data, 0, apdu, index, data.size)
//            index += data.size
//        }
//        return apdu
    }

    private fun writeLength(stream: ByteArrayOutputStream, lc: Int) {
//        if (lc < 256) {
//            stream.write(lc)
//        } else {
            stream.write(0)
            stream.write(lc shr 8)
            stream.write(lc and 0xFF)
//        }
    }


    private fun encrypt() {

    }

    companion object {
        const val ISO_CLA = 0x00.toByte()
    }
}